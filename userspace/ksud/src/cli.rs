use anyhow::{Context, Ok, Result};
use clap::Parser;
use std::path::PathBuf;

use android_logger::Config;
use log::{LevelFilter, error, info, warn};

use crate::boot_patch::{BootPatchArgs, BootRestoreArgs};
use crate::lkm_image::BootPatchV2Args;
use crate::module::regenerate_preinit_rc;
use crate::{
    apk_sign, assets, builtin_mount, cpu_spoof, debug, defs, epkesu_hide, init_event, kpatch_next,
    kpm, ksu_uapi, ksucalls, module, module_config, pathmask, rescue, sulog, utils,
};

/// KernelSU userspace cli
#[derive(Parser, Debug)]
#[command(author, version = defs::VERSION_NAME, about, long_about = None)]
struct Args {
    #[command(subcommand)]
    command: Commands,
}

#[derive(clap::Subcommand, Debug)]
enum Commands {
    /// Manage KernelSU modules
    Module {
        #[command(subcommand)]
        command: Module,
    },

    /// Manage built-in Hybrid Mount Lite
    BuiltinMount {
        #[command(subcommand)]
        command: BuiltinMount,
    },

    /// Manage built-in KPatch Next
    KpatchNext {
        #[command(subcommand)]
        command: KpatchNext,
    },

    /// Manage KPatch-Next KPM modules
    Kpm {
        #[command(subcommand)]
        command: Kpm,
    },

    /// Manage ApkeSU Hide
    EpkesuHide {
        #[command(subcommand)]
        command: EpkesuHide,
    },

    /// Manage persistent CPU model spoofing
    CpuSpoof {
        #[command(subcommand)]
        command: CpuSpoof,
    },

    /// Manage built-in pathmask LKM
    Pathmask {
        #[command(subcommand)]
        command: Pathmask,
    },

    /// Manage boot rescue protection
    Rescue {
        #[command(subcommand)]
        command: Rescue,
    },

    /// Trigger `post-fs-data` event
    PostFsData,

    /// Trigger `service` event
    Services,

    /// Run sulog reader daemon. Not for user. Use `ksud debug sulogd` to launch daemon.
    #[command(hide = true)]
    Sulogd,

    /// Trigger `boot-complete` event
    BootCompleted,

    /// Load kernelsu.ko and execute late-load stage scripts
    LateLoad {
        /// Use adb root to execute late-load for jailbreaking by Magica
        #[arg(long, default_missing_value = "5555", num_args = 0..=1)]
        magica: Option<u16>,

        /// Pass allow_shell=1 when loading kernelsu.ko
        #[arg(long)]
        allow_shell: bool,

        /// Restore adb properties after magica late-load
        #[arg(long)]
        post_magica: bool,

        /// Specify kernel KMI version instead of auto-detection
        #[arg(long)]
        kmi: Option<String>,

        /// manager package name
        #[arg(long, default_value_t = String::from(defs::DEFAULT_MANAGER_PACKAGE))]
        package_name: String,

        /// manager uid supplied by app zygote preload
        #[arg(long)]
        manager_uid: Option<u32>,
    },

    /// Register this Manager with an already loaded KernelSU kernel
    #[command(hide = true)]
    RegisterManager {
        /// manager package name
        #[arg(long, default_value_t = String::from(defs::DEFAULT_MANAGER_PACKAGE))]
        package_name: String,

        /// manager uid supplied by the Manager app
        #[arg(long)]
        manager_uid: Option<u32>,
    },

    /// Emulate system reboot
    SoftReboot,

    /// Load a kernel module with kallsyms access
    Insmod {
        /// kernel module path
        module: PathBuf,
        /// module load parameters (e.g. key=val key2=val2)
        #[arg(trailing_var_arg = true, allow_hyphen_values = true, num_args = 0..)]
        params: Vec<String>,
    },

    /// Install KernelSU userspace component to system
    Install {
        #[arg(long, default_value = None)]
        libadbroot: Option<PathBuf>,

        #[arg(long, default_value = None)]
        data_path: Option<PathBuf>,
    },

    /// Unload KernelSU kernel module (LKM Only)
    Unload,

    /// Uninstall KernelSU modules and itself(LKM Only)
    Uninstall {
        #[arg(long, default_value_t = String::from(defs::DEFAULT_MANAGER_PACKAGE))]
        package_name: String,
    },

    /// SELinux policy Patch tool
    Sepolicy {
        #[command(subcommand)]
        command: Sepolicy,
    },

    /// Manage App Profiles
    Profile {
        #[command(subcommand)]
        command: Profile,
    },

    /// Manage kernel features
    Feature {
        #[command(subcommand)]
        command: Feature,
    },

    /// Patch boot or init_boot images to apply KernelSU
    BootPatch(BootPatchArgs),

    /// Restore boot or init_boot images patched by KernelSU
    BootRestore(BootRestoreArgs),

    /// Patch the KernelSU LKM directly into a boot image
    ///
    /// This path always targets boot and never selects init_boot or vendor_boot.
    BootPatchV2(BootPatchV2Args),

    /// Show boot information
    BootInfo {
        #[command(subcommand)]
        command: BootInfo,
    },
    /// For developers
    Debug {
        #[command(subcommand)]
        command: Debug,
    },
    /// Kernel interface
    Kernel {
        #[command(subcommand)]
        command: Kernel,
    },

    /// Resetprop - Magisk-compatible system property tool
    #[command(disable_help_flag = true)]
    Resetprop {
        /// Arguments passed to resetprop
        #[arg(trailing_var_arg = true, allow_hyphen_values = true, num_args = 0..)]
        args: Vec<String>,
    },

    /// Manage initrc injection
    Initrc {
        #[command(subcommand)]
        command: Initrc,
    },
}

#[derive(clap::Subcommand, Debug)]
enum BootInfo {
    /// show current kmi version
    CurrentKmi,

    /// show supported kmi versions
    SupportedKmis,

    /// detect the KMI embedded in a boot image
    ImageKmi {
        /// boot image to inspect
        #[arg(short, long)]
        boot: std::path::PathBuf,
    },

    /// check if device is A/B capable
    IsAbDevice,

    /// show auto-selected boot partition name for current or OTA toggled slot
    DefaultPartition {
        /// toggle to another slot
        #[arg(short = 'u', long, default_value = "false")]
        ota: bool,
    },

    /// list available partitions for current or OTA toggled slot
    AvailablePartitions {
        /// toggle to another slot
        #[arg(short = 'u', long, default_value = "false")]
        ota: bool,
    },

    /// show slot suffix for current or OTA toggled slot
    SlotSuffix {
        /// toggle to another slot
        #[arg(short = 'u', long, default_value = "false")]
        ota: bool,
    },
}

#[derive(clap::Subcommand, Debug)]
enum Rescue {
    /// Print rescue protection status
    Status,

    /// Print rescue environment test report
    Test,

    /// Fully verify all rescue backups and persist the verification marker
    Verify,

    /// Import rescue config JSON from an argument
    ImportConfigJson {
        /// JSON config text
        json: String,
    },

    /// Import a user supplied partition image as rescue backup
    ImportImage {
        /// Partition name: boot/init_boot/vendor_boot/dtbo/vbmeta
        partition: String,

        /// Source image file path
        source: PathBuf,

        /// Overwrite existing rescue backup
        #[arg(long)]
        force: bool,
    },

    /// Backup boot/vendor_boot/init_boot partitions
    Backup {
        /// Overwrite existing rescue backups
        #[arg(long)]
        force: bool,
    },

    /// Enable rescue protection
    Enable,

    /// Recheck, replace backups, fully verify, and enable protection
    RefreshEnable,

    /// Disable rescue protection
    Disable,

    /// Restore boot images without touching user data
    Restore,

    /// Restore boot images without touching user data
    RestoreKeepData,

    /// Mark the next boot as pending after an external boot image change
    #[command(hide = true)]
    MarkPending {
        #[arg(default_value = "external boot image change")]
        reason: String,
    },

    /// Check rescue protection during recovery boot
    #[command(hide = true)]
    RecoveryCheck,

    /// Print rescue logs
    Logs,

    /// Clear rescue logs
    ClearLogs,

    /// Re-enable a module that rescue protection disabled
    EnableModule { id: String },

    /// Print status and logs as a diagnostic bundle
    Diagnostics,
}

#[derive(clap::Subcommand, Debug)]
enum Kpm {
    /// Print KPatch-Next KPM loader capabilities
    Caps,
    /// Print or change the overall KPM policy
    Policy {
        #[command(subcommand)]
        command: KpmPolicy,
    },
    /// List imported and loaded KPMs as JSON
    List,
    /// Print details for a KPM as JSON
    Info { id: String },
    /// Import a relocatable KPM ELF; --trusted acknowledges executable kernel code
    Import {
        file: PathBuf,
        #[arg(long, default_value = "")]
        args: String,
        #[arg(long)]
        trusted: bool,
        #[arg(long)]
        force: bool,
        #[arg(long)]
        enable: bool,
    },
    /// Enable and load an imported KPM
    Enable { id: String },
    /// Disable and unload an imported KPM
    Disable { id: String },
    /// Remove an imported KPM
    Remove { id: String },
    /// Load an enabled imported KPM without changing its enabled state
    Load { id: String },
    /// Unload a live KPM
    Unload { id: String },
    /// Invoke a KPM control callback
    Control {
        id: String,
        #[arg(long, default_value = "")]
        args: String,
    },
    /// List package UIDs excluded from KPM hooks
    ExcludeList,
    /// Set or clear a package UID exclusion
    Exclude {
        package: String,
        uid: u32,
        #[arg(long, default_value_t = true, action = clap::ArgAction::Set)]
        enabled: bool,
    },
}

#[derive(clap::Subcommand, Debug)]
enum KpmPolicy {
    /// Print the effective KPM policy
    Status,
    /// Allow KPM imports and loads through KPatch-Next
    Enable,
    /// Stop and disallow KPM loads
    Disable,
}

#[derive(clap::Subcommand, Debug)]
enum Debug {
    /// Set the manager app, kernel CONFIG_KSU_DEBUG should be enabled.
    SetManager {
        /// manager package name
        #[arg(default_value_t = String::from(defs::DEFAULT_MANAGER_PACKAGE))]
        apk: String,
    },

    /// Get apk size and hash
    GetSign {
        /// apk path
        apk: String,
    },

    /// Root Shell
    Su {
        /// switch to gloabl mount namespace
        #[arg(short, long, default_value = "false")]
        global_mnt: bool,
    },

    /// Get kernel version
    Version,

    /// Get ksud userspace version as JSON
    UserspaceVersion,

    /// For testing
    Test,

    /// Extract an embedded binary to a specified path
    ExtractBinary {
        /// binary name (e.g. busybox, resetprop, bootctl)
        name: String,
        /// destination file path
        path: PathBuf,
    },

    /// Process mark management
    Mark {
        #[command(subcommand)]
        command: MarkCommand,
    },

    /// Launch sulogd daemon manually
    Sulogd,

    /// Get kernel info
    Info,
}

#[derive(clap::Subcommand, Debug)]
enum MarkCommand {
    /// Get mark status for a process (or all)
    Get {
        /// target pid (0 for total count)
        #[arg(default_value = "0")]
        pid: i32,
    },

    /// Mark a process
    Mark {
        /// target pid (0 for all processes)
        #[arg(default_value = "0")]
        pid: i32,
    },

    /// Unmark a process
    Unmark {
        /// target pid (0 for all processes)
        #[arg(default_value = "0")]
        pid: i32,
    },

    /// Refresh mark for all running processes
    Refresh,
}

#[derive(clap::Subcommand, Debug)]
enum Sepolicy {
    /// Patch sepolicy
    Patch {
        /// sepolicy statements
        sepolicy: String,
    },

    /// Apply sepolicy from file
    Apply {
        /// sepolicy file path
        file: String,
    },

    /// Check if sepolicy statement is supported/valid
    Check {
        /// sepolicy statements
        sepolicy: String,
    },
}

#[derive(clap::Subcommand, Debug)]
enum Module {
    /// Install module <ZIP>
    Install {
        /// module zip file path
        zip: String,
    },

    /// Undo module uninstall mark <id>
    UndoUninstall {
        /// module id
        id: String,
    },

    /// Uninstall module <id>
    Uninstall {
        /// module id
        id: String,
    },

    /// enable module <id>
    Enable {
        /// module id
        id: String,
    },

    /// disable module <id>
    Disable {
        // module id
        id: String,
    },

    /// run action for module <id>
    Action {
        // module id
        id: String,
    },

    /// list all modules
    List,

    /// manage module configuration
    Config {
        /// target internal module name (resolved as internal.<name>)
        #[arg(long)]
        internal: Option<String>,
        #[command(subcommand)]
        command: ModuleConfigCmd,
    },
}

#[derive(clap::Subcommand, Debug)]
enum ModuleConfigCmd {
    /// Get a config value
    Get {
        /// config key
        key: String,
    },

    /// Set a config value
    Set {
        /// config key
        key: String,
        /// config value (omit to read from stdin)
        value: Option<String>,
        /// read value from stdin (default if value not provided)
        #[arg(long)]
        stdin: bool,
        /// use temporary config (cleared on reboot)
        #[arg(short, long)]
        temp: bool,
    },

    /// List all config entries
    List,

    /// Delete a config entry
    Delete {
        /// config key
        key: String,
        /// delete from temporary config
        #[arg(short, long)]
        temp: bool,
    },

    /// Clear all config entries
    Clear {
        /// clear temporary config
        #[arg(short, long)]
        temp: bool,
    },
}

#[derive(clap::Subcommand, Debug)]
enum BuiltinMount {
    /// Print built-in mount status as JSON
    Status,

    /// Install or enable built-in Hybrid Mount Lite
    Enable,

    /// Disable built-in Hybrid Mount Lite
    Disable,

    /// Print the global default mount mode
    GetDefaultMode,

    /// Set the global default mount mode: overlay or magic
    SetDefaultMode {
        /// overlay or magic
        mode: String,
    },

    /// Set the built-in mount package variant: lite or full
    SetVariant {
        /// lite or full
        variant: String,
    },
}

#[derive(clap::Subcommand, Debug)]
enum KpatchNext {
    /// Print built-in KPatch Next status as JSON
    Status,

    /// Install or enable built-in KPatch Next
    Enable,

    /// Uninstall built-in KPatch Next on the next boot
    Disable,
}

#[derive(clap::Subcommand, Debug)]
enum EpkesuHide {
    /// Print ApkeSU Hide status as JSON
    Status,

    /// Enable ApkeSU Hide and apply it now
    Enable,

    /// Disable ApkeSU Hide
    Disable,

    /// Apply ApkeSU Hide property changes now
    Apply,
}

#[derive(clap::Subcommand, Debug)]
enum CpuSpoof {
    /// Print CPU spoof status as JSON
    Status,

    /// Save a target CPU model and apply it if enabled
    Configure {
        /// Value written to ro.soc.model
        #[arg(long)]
        model: String,
    },

    /// Enable CPU spoofing and apply the configured target
    Enable,

    /// Disable CPU spoofing and restore the current boot's original CPU value
    Disable,

    /// Restore the original CPU value and remove the saved configuration
    RestoreDefault,
}

#[derive(clap::Subcommand, Debug)]
enum Pathmask {
    /// Print pathmask config and runtime status as JSON
    Status,

    /// Import pathmask config JSON from file
    Import {
        /// config JSON file path
        file: PathBuf,
    },

    /// Import pathmask config JSON from an argument
    ImportJson {
        /// config JSON content
        json: String,
    },

    /// Apply saved config by hot-reloading pathmask LKM
    Apply,

    /// Atomically validate, apply, and commit config JSON
    ApplyJson {
        /// config JSON content
        json: String,
    },

    /// Persistently enable or disable pathmask auto-load
    SetAutoLoad {
        /// true to auto-load, false to keep the saved config disabled
        enabled: bool,
        /// optional boot delay in seconds (0 applies immediately)
        #[arg(long)]
        delay_seconds: Option<u64>,
    },

    /// Probe whether a path is visible after dropping to an Android UID
    TestVisibility {
        /// Android application UID to probe as
        #[arg(long)]
        uid: u32,

        /// Absolute path to probe
        #[arg(long)]
        path: String,
    },

    /// Unload current pathmask LKM and clear kernel hidden paths
    Unload,

    /// Delete saved, candidate, and last-good pathmask configurations
    DeleteConfig,

    /// Print manager and kernel pathmask logs
    Logs,

    /// Clear manager pathmask logs
    ClearLogs,

    /// Print status and logs as a diagnostic bundle
    Diagnostics,
}

#[derive(clap::Subcommand, Debug)]
enum Profile {
    /// get root profile's selinux policy of <package-name>
    GetSepolicy {
        /// package name
        package: String,
    },

    /// set root profile's selinux policy of <package-name> to <profile>
    SetSepolicy {
        /// package name
        package: String,
        /// policy statements
        policy: String,
    },

    /// get template of <id>
    GetTemplate {
        /// template id
        id: String,
    },

    /// set template of <id> to <template string>
    SetTemplate {
        /// template id
        id: String,
        /// template string
        template: String,
    },

    /// delete template of <id>
    DeleteTemplate {
        /// template id
        id: String,
    },

    /// list all templates
    ListTemplates,
}

#[derive(clap::Subcommand, Debug)]
enum Feature {
    /// Get feature value and support status
    Get {
        /// Feature ID or name (su_compat, kernel_umount, sulog, adb_root, selinux_hide, avc_spoof, webview_zygote_umount)
        id: String,
        /// Read from config file
        #[arg(long, default_value_t = false)]
        config: bool,
    },

    /// Set feature value
    Set {
        /// Feature ID or name
        id: String,
        /// Feature value (0=disable, 1=enable)
        value: u64,
    },

    /// List all available features
    List,

    /// Check feature status (supported/unsupported/managed)
    Check {
        /// Feature ID or name (su_compat, kernel_umount, sulog, adb_root, selinux_hide, avc_spoof, webview_zygote_umount)
        id: String,
    },

    /// Load configuration from file and apply to kernel
    Load,

    /// Save current kernel feature states to file
    Save,
}

#[derive(clap::Subcommand, Debug)]
enum Kernel {
    /// Nuke ext4 sysfs
    NukeExt4Sysfs {
        /// mount point
        mnt: String,
    },
    /// Manage umount list
    Umount {
        #[command(subcommand)]
        command: UmountOp,
    },
    /// Notify that module is mounted
    NotifyModuleMounted,
}

#[derive(clap::Subcommand, Debug)]
enum UmountOp {
    /// Add mount point to umount list
    Add {
        /// mount point path
        mnt: String,
        /// umount flags (default: 0, MNT_DETACH: 2)
        #[arg(short, long, default_value = "0")]
        flags: u32,
    },
    /// Delete mount point from umount list
    Del {
        /// mount point path
        mnt: String,
    },
    /// Wipe all entries from umount list
    Wipe,
}

#[derive(clap::Subcommand, Debug)]
enum Initrc {
    /// Regenerate preinit rc file
    Refresh,
}

pub fn run() -> Result<()> {
    android_logger::init_once(
        Config::default()
            .with_max_level(crate::debug_select!(LevelFilter::Trace, LevelFilter::Info))
            .with_tag("KernelSU"),
    );

    // the kernel executes su with argv[0] = "su" and replace it with us
    let arg0 = std::env::args().next().unwrap_or_default();
    if arg0 == "su" || arg0.ends_with("/su") {
        return crate::su::root_shell();
    }

    if arg0.ends_with("resetprop") {
        let all_args: Vec<String> = std::env::args().collect();
        crate::resetprop::resetprop_main(&all_args)
    }

    let cli = Args::parse();

    log::info!("command: {:?}", cli.command);

    let result = match cli.command {
        Commands::PostFsData => init_event::on_post_data_fs(),
        Commands::BootCompleted => {
            init_event::on_boot_completed();
            Ok(())
        }

        Commands::SoftReboot => init_event::soft_reboot(),

        Commands::Insmod { module, params } => debug::insmod(&module, &params),

        Commands::Module { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                Module::Install { zip } => module::install_module(&zip),
                Module::UndoUninstall { id } => module::undo_uninstall_module(&id),
                Module::Uninstall { id } => module::uninstall_module(&id),
                Module::Enable { id } => module::enable_module(&id),
                Module::Disable { id } => module::disable_module(&id),
                Module::Action { id } => module::run_action(&id),
                Module::List => module::list_modules(),
                Module::Config { internal, command } => {
                    let module_id = match internal {
                        Some(internal_name) => format!("internal.{internal_name}"),
                        None => std::env::var("KSU_MODULE").map_err(|_| {
                            anyhow::anyhow!(
                                "This command must be run in the context of a module or passed --internal <name>"
                            )
                        })?,
                    };
                    crate::module::validate_module_id(&module_id)?;

                    match command {
                        ModuleConfigCmd::Get { key } => {
                            // Use merge_configs to respect priority (temp overrides persist)
                            let config = module_config::merge_configs(&module_id)?;
                            match config.get(&key) {
                                Some(value) => {
                                    println!("{value}");
                                    Ok(())
                                }
                                None => anyhow::bail!("Key '{key}' not found"),
                            }
                        }
                        ModuleConfigCmd::Set {
                            key,
                            value,
                            stdin,
                            temp,
                        } => {
                            // Validate key at CLI layer for better user experience
                            module_config::validate_config_key(&key)?;

                            // Read value from stdin or argument
                            let value_str = match value {
                                Some(v) if !stdin => v,
                                _ => {
                                    // Read from stdin
                                    use std::io::Read;
                                    let mut buffer = String::new();
                                    std::io::stdin()
                                        .read_to_string(&mut buffer)
                                        .context("Failed to read from stdin")?;
                                    buffer
                                }
                            };

                            // Validate value
                            module_config::validate_config_value(&value_str)?;

                            let config_type = if temp {
                                module_config::ConfigType::Temp
                            } else {
                                module_config::ConfigType::Persist
                            };
                            module_config::set_config_value(
                                &module_id,
                                &key,
                                &value_str,
                                config_type,
                            )
                        }
                        ModuleConfigCmd::List => {
                            let config = module_config::merge_configs(&module_id)?;
                            if config.is_empty() {
                                println!("No config entries found");
                            } else {
                                for (key, value) in config {
                                    println!("{key}={value}");
                                }
                            }
                            Ok(())
                        }
                        ModuleConfigCmd::Delete { key, temp } => {
                            let config_type = if temp {
                                module_config::ConfigType::Temp
                            } else {
                                module_config::ConfigType::Persist
                            };
                            module_config::delete_config_value(&module_id, &key, config_type)
                        }
                        ModuleConfigCmd::Clear { temp } => {
                            let config_type = if temp {
                                module_config::ConfigType::Temp
                            } else {
                                module_config::ConfigType::Persist
                            };
                            module_config::clear_config(&module_id, config_type)
                        }
                    }
                }
            }
        }
        Commands::BuiltinMount { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                BuiltinMount::Status => {
                    builtin_mount::print_status();
                    Ok(())
                }
                BuiltinMount::Enable => builtin_mount::enable(),
                BuiltinMount::Disable => builtin_mount::disable(),
                BuiltinMount::GetDefaultMode => {
                    builtin_mount::print_default_mode();
                    Ok(())
                }
                BuiltinMount::SetDefaultMode { mode } => {
                    let mode = builtin_mount::MountMode::parse(&mode)?;
                    builtin_mount::set_default_mode(mode)
                }
                BuiltinMount::SetVariant { variant } => {
                    let variant = builtin_mount::BuiltinMountVariant::parse(&variant)?;
                    builtin_mount::set_variant(variant)
                }
            }
        }
        Commands::KpatchNext { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                KpatchNext::Status => {
                    kpatch_next::print_status();
                    Ok(())
                }
                KpatchNext::Enable => kpatch_next::enable(),
                KpatchNext::Disable => kpatch_next::disable(),
            }
        }
        Commands::Kpm { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                Kpm::Caps => {
                    kpm::print_caps();
                    Ok(())
                }
                Kpm::Policy { command } => match command {
                    KpmPolicy::Status => {
                        kpm::print_policy();
                        Ok(())
                    }
                    KpmPolicy::Enable => kpm::set_policy(true),
                    KpmPolicy::Disable => kpm::set_policy(false),
                },
                Kpm::List => kpm::print_list(),
                Kpm::Info { id } => kpm::print_info(&id),
                Kpm::Import {
                    file,
                    args,
                    trusted,
                    force,
                    enable,
                } => kpm::import(&file, &args, trusted, force, enable),
                Kpm::Enable { id } => kpm::enable_module(&id),
                Kpm::Disable { id } => kpm::disable_module(&id),
                Kpm::Remove { id } => kpm::remove_module(&id),
                Kpm::Load { id } => kpm::load_module(&id),
                Kpm::Unload { id } => kpm::unload_module(&id),
                Kpm::Control { id, args } => kpm::control_module(&id, &args),
                Kpm::ExcludeList => kpm::print_exclude_list(),
                Kpm::Exclude {
                    package,
                    uid,
                    enabled,
                } => kpm::set_excluded_package(&package, uid, enabled),
            }
        }
        Commands::EpkesuHide { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                EpkesuHide::Status => {
                    epkesu_hide::print_status();
                    Ok(())
                }
                EpkesuHide::Enable => epkesu_hide::enable(),
                EpkesuHide::Disable => epkesu_hide::disable(),
                EpkesuHide::Apply => epkesu_hide::apply(),
            }
        }
        Commands::CpuSpoof { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                CpuSpoof::Status => {
                    cpu_spoof::print_status();
                    Ok(())
                }
                CpuSpoof::Configure { model } => cpu_spoof::configure(&model),
                CpuSpoof::Enable => cpu_spoof::enable(),
                CpuSpoof::Disable => cpu_spoof::disable(),
                CpuSpoof::RestoreDefault => cpu_spoof::restore_default(),
            }
        }
        Commands::Pathmask { command } => {
            utils::switch_mnt_ns(1)?;
            match command {
                Pathmask::Status => {
                    pathmask::print_status();
                    Ok(())
                }
                Pathmask::Import { file } => pathmask::import_config(&file),
                Pathmask::ImportJson { json } => pathmask::import_config_text(&json),
                Pathmask::Apply => pathmask::apply(),
                Pathmask::ApplyJson { json } => pathmask::apply_config_text(&json),
                Pathmask::SetAutoLoad {
                    enabled,
                    delay_seconds,
                } => pathmask::set_auto_load(enabled, delay_seconds),
                Pathmask::TestVisibility { uid, path } => pathmask::test_visibility(uid, &path),
                Pathmask::Unload => pathmask::unload(),
                Pathmask::DeleteConfig => pathmask::delete_config(),
                Pathmask::Logs => {
                    pathmask::print_logs();
                    Ok(())
                }
                Pathmask::ClearLogs => pathmask::clear_logs(),
                Pathmask::Diagnostics => {
                    pathmask::print_diagnostics();
                    Ok(())
                }
            }
        }
        Commands::Rescue { command } => {
            if matches!(command, Rescue::RecoveryCheck) {
                if let Err(err) = utils::switch_mnt_ns(1) {
                    warn!("continue recovery rescue check without pid 1 mount namespace: {err:#}");
                }
            } else {
                utils::switch_mnt_ns(1)?;
            }
            let result = match command {
                Rescue::Status => {
                    rescue::print_status();
                    Ok(())
                }
                Rescue::Test => {
                    rescue::print_test_report();
                    Ok(())
                }
                Rescue::Verify => {
                    rescue::print_verify_report();
                    Ok(())
                }
                Rescue::ImportConfigJson { json } => rescue::import_config_text(&json),
                Rescue::ImportImage {
                    partition,
                    source,
                    force,
                } => rescue::import_image(&partition, &source, force),
                Rescue::Backup { force } => rescue::backup(force),
                Rescue::Enable => rescue::enable(),
                Rescue::RefreshEnable => rescue::refresh_and_enable(),
                Rescue::Disable => rescue::disable(),
                Rescue::Restore => rescue::restore_now(),
                Rescue::RestoreKeepData => rescue::restore_keep_data_now(),
                Rescue::MarkPending { reason } => rescue::mark_next_boot_pending(&reason),
                Rescue::RecoveryCheck => {
                    rescue::check_on_recovery_boot();
                    Ok(())
                }
                Rescue::Logs => {
                    rescue::print_logs();
                    Ok(())
                }
                Rescue::ClearLogs => rescue::clear_logs(),
                Rescue::EnableModule { id } => rescue::enable_rescue_module(&id),
                Rescue::Diagnostics => {
                    rescue::print_diagnostics();
                    Ok(())
                }
            };
            result.map_err(rescue::structured_error)
        }
        Commands::Install {
            libadbroot,
            data_path,
        } => utils::install(libadbroot, data_path),
        Commands::Unload => crate::unload::unload(),
        Commands::Uninstall { package_name } => utils::uninstall(&package_name),
        Commands::Sepolicy { command } => match command {
            Sepolicy::Patch { sepolicy } => crate::sepolicy::live_patch(&sepolicy),
            Sepolicy::Apply { file } => crate::sepolicy::apply_file(file),
            Sepolicy::Check { sepolicy } => crate::sepolicy::check_rule(&sepolicy),
        },
        Commands::LateLoad {
            magica,
            allow_shell,
            post_magica,
            kmi,
            package_name,
            manager_uid,
        } => {
            if let Some(port) = magica {
                return crate::magica::run(port, &package_name, manager_uid, allow_shell).map_err(
                    |e| {
                        error!("Error running magica: {e}");
                        e
                    },
                );
            }
            let result = crate::late_load::run(&package_name, manager_uid, kmi, allow_shell);
            if post_magica {
                info!("Restoring adb properties (post-magica cleanup)...");
                if let Err(e) = crate::magica::disable_adb_root() {
                    error!("disable adb root failed: {e}");
                }
            }
            result
        }
        Commands::RegisterManager {
            package_name,
            manager_uid,
        } => crate::late_load::register_manager(&package_name, manager_uid),
        Commands::Services => {
            if ksucalls::get_version() <= 0 {
                info!("KernelSU not available, exiting services");
                std::process::exit(0);
            }
            init_event::on_services();
            Ok(())
        }
        Commands::Sulogd => sulog::run_sulogd(),
        Commands::Profile { command } => match command {
            Profile::GetSepolicy { package } => crate::profile::get_sepolicy(&package),
            Profile::SetSepolicy { package, policy } => {
                crate::profile::set_sepolicy(&package, policy)
            }
            Profile::GetTemplate { id } => crate::profile::get_template(&id),
            Profile::SetTemplate { id, template } => crate::profile::set_template(&id, template),
            Profile::DeleteTemplate { id } => crate::profile::delete_template(&id),
            Profile::ListTemplates => crate::profile::list_templates(),
        },

        Commands::Feature { command } => match command {
            Feature::Get { id, config } => {
                if config {
                    crate::feature::get_feature_config(&id)
                } else {
                    crate::feature::get_feature(&id)
                }
            }
            Feature::Set { id, value } => crate::feature::set_feature(&id, value),
            Feature::List => {
                crate::feature::list_features();
                Ok(())
            }
            Feature::Check { id } => crate::feature::check_feature(&id),
            Feature::Load => crate::feature::load_config_and_apply(),
            Feature::Save => crate::feature::save_config(),
        },

        Commands::Debug { command } => match command {
            Debug::SetManager { apk } => debug::set_manager(&apk),
            Debug::GetSign { apk } => {
                let sign = apk_sign::get_apk_signature(&apk)?;
                println!("size: {:#x}, hash: {}", sign.0, sign.1);
                Ok(())
            }
            Debug::Version => {
                println!("Kernel Version: {}", ksucalls::get_version());
                Ok(())
            }
            Debug::UserspaceVersion => {
                println!(
                    "{}",
                    serde_json::json!({
                        "versionCode": defs::VERSION_CODE.trim(),
                        "versionName": defs::VERSION_NAME.trim(),
                    })
                );
                Ok(())
            }
            Debug::Su { global_mnt } => crate::su::grant_root(global_mnt),
            Debug::Test => assets::ensure_binaries(false),
            Debug::ExtractBinary { name, path } => {
                let data = assets::get_asset_data(&name)?;
                utils::ensure_binary(&path, &data, false)
            }
            Debug::Mark { command } => match command {
                MarkCommand::Get { pid } => debug::mark_get(pid),
                MarkCommand::Mark { pid } => debug::mark_set(pid),
                MarkCommand::Unmark { pid } => debug::mark_unset(pid),
                MarkCommand::Refresh => debug::mark_refresh(),
            },
            Debug::Sulogd => sulog::ensure_sulogd_running(),
            Debug::Info => {
                let info = ksucalls::get_info();
                println!("version: {}", info.version);
                println!("flags: 0x{:x}", info.flags);
                println!("uapi_version: {}", info.uapi_version);
                println!("features: 0x{:x}", info.features);
                println!(
                    "lkm: {}",
                    (info.flags & ksu_uapi::KSU_GET_INFO_FLAG_LKM) != 0
                );
                println!(
                    "late_load: {}",
                    (info.flags & ksu_uapi::KSU_GET_INFO_FLAG_LATE_LOAD) != 0
                );
                println!(
                    "pr_build: {}",
                    (info.flags & ksu_uapi::KSU_GET_INFO_FLAG_PR_BUILD) != 0
                );
                Ok(())
            }
        },

        Commands::BootPatch(boot_patch) => crate::boot_patch::patch(boot_patch),

        Commands::BootPatchV2(boot_patch) => crate::lkm_image::patch_boot(&boot_patch),

        Commands::BootInfo { command } => match command {
            BootInfo::CurrentKmi => {
                let kmi = crate::boot_patch::get_current_kmi()?;
                println!("{kmi}");
                // return here to avoid printing the error message
                return Ok(());
            }
            BootInfo::SupportedKmis => {
                let kmi = crate::assets::list_supported_kmi();
                for kmi in &kmi {
                    println!("{kmi}");
                }
                return Ok(());
            }
            BootInfo::ImageKmi { boot } => {
                let kmi = crate::boot_patch::get_kmi_from_boot(&boot)?;
                println!("{kmi}");
                return Ok(());
            }
            BootInfo::IsAbDevice => {
                let val = crate::utils::getprop("ro.build.ab_update")
                    .unwrap_or_else(|| String::from("false"));
                let slot_suffix = crate::boot_patch::get_slot_suffix(false).unwrap_or_default();
                let is_ab = val.trim().eq_ignore_ascii_case("true")
                    || matches!(slot_suffix.as_str(), "_a" | "_b");
                println!("{}", if is_ab { "true" } else { "false" });
                return Ok(());
            }
            BootInfo::DefaultPartition { ota } => {
                let kmi = crate::boot_patch::get_current_kmi().unwrap_or_else(|_| String::new());
                let name = crate::boot_patch::choose_boot_partition(&kmi, false, &None, ota);
                println!("{name}");
                return Ok(());
            }
            BootInfo::SlotSuffix { ota } => {
                let suffix = crate::boot_patch::get_slot_suffix(ota)?;
                println!("{suffix}");
                return Ok(());
            }
            BootInfo::AvailablePartitions { ota } => {
                let parts = crate::boot_patch::list_available_partitions(ota);
                for p in &parts {
                    println!("{p}");
                }
                return Ok(());
            }
        },
        Commands::BootRestore(boot_restore) => crate::boot_patch::restore(boot_restore),
        Commands::Resetprop { args } => {
            let mut full_args = vec!["resetprop".to_string()];
            full_args.extend(args);
            crate::resetprop::resetprop_main(&full_args)
        }

        Commands::Kernel { command } => match command {
            Kernel::NukeExt4Sysfs { mnt } => ksucalls::nuke_ext4_sysfs(&mnt),
            Kernel::Umount { command } => match command {
                UmountOp::Add { mnt, flags } => ksucalls::umount_list_add(&mnt, flags),
                UmountOp::Del { mnt } => ksucalls::umount_list_del(&mnt),
                UmountOp::Wipe => ksucalls::umount_list_wipe().map_err(Into::into),
            },
            Kernel::NotifyModuleMounted => {
                ksucalls::report_module_mounted();
                Ok(())
            }
        },
        Commands::Initrc { command } => match command {
            Initrc::Refresh => regenerate_preinit_rc(),
        },
    };

    if let Err(e) = &result {
        log::error!("Error: {e:?}");
    }
    result
}
