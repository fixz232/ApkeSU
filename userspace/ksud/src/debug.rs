use anyhow::{Context, Result, bail, ensure};
use std::{
    ffi::CString,
    fs,
    path::{Path, PathBuf},
    process::Command,
};

use crate::ksucalls;

const KERNEL_MODULE_NAMES: [&str; 2] = ["kernelsu", "apkesu"];
const FIRST_APPLICATION_APPID: u32 = 10_000;
const LAST_APPLICATION_APPID: u32 = 19_999;

fn read_i32(path: &PathBuf) -> Result<i32> {
    let content = std::fs::read_to_string(path)?;
    let content = content.trim();
    let content = content.parse::<i32>()?;
    Ok(content)
}

fn set_kernel_param(appid: u32) -> Result<()> {
    if !(FIRST_APPLICATION_APPID..=LAST_APPLICATION_APPID).contains(&appid) {
        bail!("refusing non-application manager appid {appid}");
    }

    let mut errors = Vec::new();
    for module_name in KERNEL_MODULE_NAMES {
        let parameter = Path::new("/sys/module")
            .join(module_name)
            .join("parameters")
            .join("manager_appid");
        match (|| -> Result<()> {
            let before_appid = read_i32(&parameter)?;
            std::fs::write(&parameter, appid.to_string())?;
            let after_appid = read_i32(&parameter)?;
            ensure!(
                after_appid == appid as i32,
                "manager appid verification failed: {after_appid}"
            );
            let registered_appid = ksucalls::get_manager_appid()?;
            ensure!(
                registered_appid == appid,
                "kernel manager appid verification failed: {registered_appid}"
            );
            println!("set manager appid: {before_appid} -> {after_appid}");
            Ok(())
        })() {
            Ok(()) => return Ok(()),
            Err(error) => errors.push(format!("{}: {error:#}", parameter.display())),
        }
    }
    bail!("could not set manager appid: {}", errors.join("; "))
}

fn get_pkg_appid(pkg: &str) -> Result<u32> {
    // stat /data/data/<pkg>
    let uid = rustix::fs::stat(format!("/data/data/{pkg}"))
        .with_context(|| format!("stat /data/data/{pkg}"))?
        .st_uid as u32;
    Ok(uid % 100_000)
}

pub fn set_manager(pkg: &str) -> Result<()> {
    let appid = get_pkg_appid(pkg)?;
    set_kernel_param(appid)?;
    // force-stop it
    let _ = Command::new("am").args(["force-stop", pkg]).status();
    Ok(())
}

pub fn insmod(module: &Path, params: &[String]) -> Result<()> {
    let module = module
        .canonicalize()
        .with_context(|| format!("resolve module path failed: {}", module.display()))?;
    let module_data =
        fs::read(&module).with_context(|| format!("read module failed: {}", module.display()))?;
    let cparams = CString::new(params.join(" "))?;

    ksuinit::load_module(&module_data, &cparams)
        .with_context(|| format!("load module failed: {}", module.display()))?;

    println!("Loaded kernel module: {}", module.display());
    Ok(())
}

/// Get mark status for a process
pub fn mark_get(pid: i32) -> Result<()> {
    let result = ksucalls::mark_get(pid)?;
    if pid == 0 {
        bail!("Please specify a pid to get its mark status");
    }
    println!(
        "Process {pid} mark status: {}",
        if result != 0 { "marked" } else { "unmarked" }
    );
    Ok(())
}

/// Mark a process
pub fn mark_set(pid: i32) -> Result<()> {
    ksucalls::mark_set(pid)?;
    if pid == 0 {
        println!("All processes marked successfully");
    } else {
        println!("Process {pid} marked successfully");
    }
    Ok(())
}

/// Unmark a process
pub fn mark_unset(pid: i32) -> Result<()> {
    ksucalls::mark_unset(pid)?;
    if pid == 0 {
        println!("All processes unmarked successfully");
    } else {
        println!("Process {pid} unmarked successfully");
    }
    Ok(())
}

/// Refresh mark for all running processes
pub fn mark_refresh() -> Result<()> {
    ksucalls::mark_refresh()?;
    println!("Refreshed mark for all running processes");
    Ok(())
}
