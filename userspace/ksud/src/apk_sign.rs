use anyhow::{Context, Result, bail, ensure};
use std::fs::File;
use std::io::{Read, Seek, SeekFrom};

const EOCD_LEN: u64 = 22;
const MAX_ZIP_COMMENT: u64 = u16::MAX as u64;
const APK_SIG_MAGIC: &[u8; 16] = b"APK Sig Block 42";
const APK_V2_BLOCK_ID: u32 = 0x7109_871a;
const APK_V3_BLOCK_ID: u32 = 0xf053_68c0;
const APK_V31_BLOCK_ID: u32 = 0x1b93_ad61;
const MIN_CERT_SIZE: u32 = 0x100;
const MAX_CERT_SIZE: u32 = 0x1000;
const ZIP_CENTRAL_DIRECTORY_HEADER_SIZE: u64 = 46;
const ZIP_CENTRAL_DIRECTORY_MAGIC: u32 = 0x0201_4b50;
const V1_MANIFEST: &[u8] = b"META-INF/MANIFEST.MF";

fn read_exact_at(file: &mut File, offset: u64, bytes: &mut [u8]) -> Result<()> {
    file.seek(SeekFrom::Start(offset))?;
    file.read_exact(bytes)?;
    Ok(())
}

fn read_u32(file: &mut File, position: &mut u64, end: u64) -> Result<u32> {
    ensure!(
        end.saturating_sub(*position) >= 4,
        "truncated APK signing block"
    );
    let mut bytes = [0u8; 4];
    read_exact_at(file, *position, &mut bytes)?;
    *position += 4;
    Ok(u32::from_le_bytes(bytes))
}

fn read_u64(file: &mut File, position: &mut u64, end: u64) -> Result<u64> {
    ensure!(
        end.saturating_sub(*position) >= 8,
        "truncated APK signing block"
    );
    let mut bytes = [0u8; 8];
    read_exact_at(file, *position, &mut bytes)?;
    *position += 8;
    Ok(u64::from_le_bytes(bytes))
}

fn length_prefixed_end(file: &mut File, position: &mut u64, end: u64) -> Result<u64> {
    let length = u64::from(read_u32(file, position, end)?);
    let value_end = position
        .checked_add(length)
        .context("APK signing block length overflow")?;
    ensure!(
        value_end <= end,
        "APK signing block entry exceeds its container"
    );
    Ok(value_end)
}

fn find_eocd(file: &mut File, file_size: u64) -> Result<(u64, u64, u64)> {
    ensure!(file_size >= EOCD_LEN, "not a ZIP file");
    let max_comment = MAX_ZIP_COMMENT.min(file_size - EOCD_LEN);
    let mut record = [0u8; EOCD_LEN as usize];

    for comment_length in 0..=max_comment {
        let offset = file_size - EOCD_LEN - comment_length;
        read_exact_at(file, offset, &mut record)?;
        if record[0..4] != [0x50, 0x4b, 0x05, 0x06] {
            continue;
        }
        let declared_comment = u64::from(u16::from_le_bytes([record[20], record[21]]));
        if declared_comment != comment_length {
            continue;
        }
        let directory_size = u64::from(u32::from_le_bytes(record[12..16].try_into()?));
        let directory_offset = u64::from(u32::from_le_bytes(record[16..20].try_into()?));
        ensure!(
            directory_offset.checked_add(directory_size) == Some(offset),
            "invalid ZIP central directory"
        );
        return Ok((offset, directory_offset, directory_size));
    }
    bail!("ZIP end-of-central-directory record not found")
}

fn has_v1_signature_file(
    file: &mut File,
    directory_offset: u64,
    directory_size: u64,
) -> Result<bool> {
    let directory_end = directory_offset
        .checked_add(directory_size)
        .context("ZIP central directory length overflow")?;
    let mut position = directory_offset;
    let mut header = [0u8; ZIP_CENTRAL_DIRECTORY_HEADER_SIZE as usize];

    while position < directory_end {
        ensure!(
            directory_end.saturating_sub(position) >= ZIP_CENTRAL_DIRECTORY_HEADER_SIZE,
            "truncated ZIP central directory entry"
        );
        read_exact_at(file, position, &mut header)?;
        ensure!(
            u32::from_le_bytes(header[0..4].try_into()?) == ZIP_CENTRAL_DIRECTORY_MAGIC,
            "invalid ZIP central directory entry"
        );
        let file_name_length = u64::from(u16::from_le_bytes(header[28..30].try_into()?));
        let extra_field_length = u64::from(u16::from_le_bytes(header[30..32].try_into()?));
        let comment_length = u64::from(u16::from_le_bytes(header[32..34].try_into()?));
        position += ZIP_CENTRAL_DIRECTORY_HEADER_SIZE;
        let variable_size = file_name_length
            .checked_add(extra_field_length)
            .and_then(|size| size.checked_add(comment_length))
            .context("ZIP central directory entry length overflow")?;
        ensure!(
            variable_size <= directory_end.saturating_sub(position),
            "ZIP central directory entry exceeds its container"
        );

        if file_name_length == V1_MANIFEST.len() as u64 {
            let mut file_name = [0u8; V1_MANIFEST.len()];
            read_exact_at(file, position, &mut file_name)?;
            if file_name == V1_MANIFEST {
                return Ok(true);
            }
        }
        position += variable_size;
    }

    ensure!(
        position == directory_end,
        "invalid ZIP central directory size"
    );
    Ok(false)
}

fn certificate_signature(file: &mut File, start: u64, end: u64) -> Result<(u32, String)> {
    let mut position = start;
    let signer_sequence_end = length_prefixed_end(file, &mut position, end)?;
    let first_signer_end = length_prefixed_end(file, &mut position, signer_sequence_end)?;
    ensure!(
        first_signer_end == signer_sequence_end,
        "APK must contain exactly one v2 signer"
    );
    let signed_data_end = length_prefixed_end(file, &mut position, first_signer_end)?;
    let digests_end = length_prefixed_end(file, &mut position, signed_data_end)?;
    position = digests_end;
    let certificates_end = length_prefixed_end(file, &mut position, signed_data_end)?;
    let certificate_size = read_u32(file, &mut position, certificates_end)?;
    ensure!(
        (MIN_CERT_SIZE..=MAX_CERT_SIZE).contains(&certificate_size),
        "APK signer certificate size is outside the supported range"
    );
    ensure!(
        u64::from(certificate_size) <= certificates_end.saturating_sub(position),
        "truncated APK signer certificate"
    );
    let mut certificate = vec![0; certificate_size as usize];
    read_exact_at(file, position, &mut certificate)?;
    Ok((certificate_size, sha256::digest(&certificate)))
}

pub fn get_apk_signature(apk: &str) -> Result<(u32, String)> {
    let mut file = File::open(apk).with_context(|| format!("failed to open APK {apk}"))?;
    let file_size = file.metadata()?.len();
    let (eocd_offset, directory_offset, directory_size) = find_eocd(&mut file, file_size)?;

    if eocd_offset >= 20 {
        let mut zip64_magic = [0u8; 4];
        read_exact_at(&mut file, eocd_offset - 20, &mut zip64_magic)?;
        ensure!(
            u32::from_le_bytes(zip64_magic) != 0x0706_4b50,
            "ZIP64 APKs are not supported"
        );
    }

    ensure!(directory_offset >= 32, "APK has no signing block");
    let footer_offset = directory_offset - 24;
    let mut footer = [0u8; 24];
    read_exact_at(&mut file, footer_offset, &mut footer)?;
    let block_size = u64::from_le_bytes(footer[0..8].try_into()?);
    ensure!(
        &footer[8..24] == APK_SIG_MAGIC,
        "APK signing block was not found"
    );
    ensure!(
        block_size >= 24 && block_size <= directory_offset.saturating_sub(8),
        "invalid APK signing block size"
    );
    let block_start = directory_offset - block_size - 8;
    let mut head = [0u8; 8];
    read_exact_at(&mut file, block_start, &mut head)?;
    ensure!(
        u64::from_le_bytes(head) == block_size,
        "APK signing block size mismatch"
    );

    let signing_pairs_end = footer_offset;
    let mut position = block_start + 8;
    let mut v2_signature = None;
    let mut v2_count = 0;
    let mut has_v3 = false;
    while position < signing_pairs_end {
        let pair_size = read_u64(&mut file, &mut position, signing_pairs_end)?;
        ensure!(pair_size >= 4, "invalid APK signing pair size");
        let current_pair_end = position
            .checked_add(pair_size)
            .context("APK signing pair length overflow")?;
        ensure!(
            current_pair_end <= signing_pairs_end,
            "APK signing pair exceeds block"
        );
        let id = read_u32(&mut file, &mut position, current_pair_end)?;
        match id {
            APK_V2_BLOCK_ID => {
                v2_count += 1;
                v2_signature = Some(certificate_signature(
                    &mut file,
                    position,
                    current_pair_end,
                )?);
            }
            APK_V3_BLOCK_ID | APK_V31_BLOCK_ID => has_v3 = true,
            _ => {}
        }
        position = current_pair_end;
    }

    ensure!(
        v2_count == 1,
        "APK must contain exactly one v2 signing block"
    );
    ensure!(
        !has_v3,
        "APK v3/v3.1 signatures are not supported by this kernel verifier"
    );
    ensure!(
        !has_v1_signature_file(&mut file, directory_offset, directory_size)?,
        "APK v1 signatures are not supported by this kernel verifier"
    );
    v2_signature.context("APK v2 signer certificate was not found")
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    fn length_prefixed(value: &[u8]) -> Vec<u8> {
        let mut result = Vec::with_capacity(4 + value.len());
        result.extend_from_slice(&(value.len() as u32).to_le_bytes());
        result.extend_from_slice(value);
        result
    }

    fn test_apk(certificates: &[&[u8]], include_v1_manifest: bool) -> tempfile::NamedTempFile {
        let digests = length_prefixed(&[]);
        let certificate_entries = certificates
            .iter()
            .flat_map(|certificate| length_prefixed(certificate))
            .collect::<Vec<_>>();
        let certificate_sequence = length_prefixed(&certificate_entries);
        let mut signed_data_value = digests;
        signed_data_value.extend_from_slice(&certificate_sequence);
        let signed_data = length_prefixed(&signed_data_value);
        let signer = length_prefixed(&signed_data);
        let signers = length_prefixed(&signer);

        let mut pair_value = APK_V2_BLOCK_ID.to_le_bytes().to_vec();
        pair_value.extend_from_slice(&signers);
        let mut pairs = (pair_value.len() as u64).to_le_bytes().to_vec();
        pairs.extend_from_slice(&pair_value);

        let block_size = (pairs.len() + 24) as u64;
        let mut apk = block_size.to_le_bytes().to_vec();
        apk.extend_from_slice(&pairs);
        apk.extend_from_slice(&block_size.to_le_bytes());
        apk.extend_from_slice(APK_SIG_MAGIC);
        let central_directory_offset = apk.len() as u32;

        if include_v1_manifest {
            let mut header = [0u8; ZIP_CENTRAL_DIRECTORY_HEADER_SIZE as usize];
            header[0..4].copy_from_slice(&ZIP_CENTRAL_DIRECTORY_MAGIC.to_le_bytes());
            header[28..30].copy_from_slice(&(V1_MANIFEST.len() as u16).to_le_bytes());
            apk.extend_from_slice(&header);
            apk.extend_from_slice(V1_MANIFEST);
        }
        let central_directory_size = apk.len() as u32 - central_directory_offset;

        apk.extend_from_slice(&0x0605_4b50u32.to_le_bytes());
        apk.extend_from_slice(&[0; 8]);
        apk.extend_from_slice(&central_directory_size.to_le_bytes());
        apk.extend_from_slice(&central_directory_offset.to_le_bytes());
        apk.extend_from_slice(&0u16.to_le_bytes());

        let mut temporary = tempfile::NamedTempFile::new().unwrap();
        temporary.write_all(&apk).unwrap();
        temporary.flush().unwrap();
        temporary
    }

    #[test]
    fn rejects_non_zip_input() {
        let temporary = tempfile::NamedTempFile::new().unwrap();
        assert!(get_apk_signature(temporary.path().to_str().unwrap()).is_err());
    }

    #[test]
    fn parses_a_single_v2_signer_certificate() {
        let certificate = vec![0x5a; MIN_CERT_SIZE as usize];
        let apk = test_apk(&[&certificate], false);
        assert_eq!(
            get_apk_signature(apk.path().to_str().unwrap()).unwrap(),
            (MIN_CERT_SIZE, sha256::digest(&certificate)),
        );
    }

    #[test]
    fn rejects_v1_and_v2_mixed_signatures() {
        let certificate = vec![0x5a; MIN_CERT_SIZE as usize];
        let apk = test_apk(&[&certificate], true);
        assert!(get_apk_signature(apk.path().to_str().unwrap()).is_err());
    }

    #[test]
    fn rejects_multiple_v2_signers() {
        let certificate = vec![0x5a; MIN_CERT_SIZE as usize];
        let digests = length_prefixed(&[]);
        let certificate_entry = length_prefixed(&certificate);
        let certificate_sequence = length_prefixed(&certificate_entry);
        let mut signed_data_value = digests;
        signed_data_value.extend_from_slice(&certificate_sequence);
        let signer = length_prefixed(&length_prefixed(&signed_data_value));
        let mut signers_value = signer.clone();
        signers_value.extend_from_slice(&signer);
        let signers = length_prefixed(&signers_value);

        let mut pair_value = APK_V2_BLOCK_ID.to_le_bytes().to_vec();
        pair_value.extend_from_slice(&signers);
        let mut pairs = (pair_value.len() as u64).to_le_bytes().to_vec();
        pairs.extend_from_slice(&pair_value);
        let block_size = (pairs.len() + 24) as u64;
        let mut apk_bytes = block_size.to_le_bytes().to_vec();
        apk_bytes.extend_from_slice(&pairs);
        apk_bytes.extend_from_slice(&block_size.to_le_bytes());
        apk_bytes.extend_from_slice(APK_SIG_MAGIC);
        let central_directory_offset = apk_bytes.len() as u32;
        apk_bytes.extend_from_slice(&0x0605_4b50u32.to_le_bytes());
        apk_bytes.extend_from_slice(&[0; 12]);
        apk_bytes.extend_from_slice(&central_directory_offset.to_le_bytes());
        apk_bytes.extend_from_slice(&0u16.to_le_bytes());

        let mut apk = tempfile::NamedTempFile::new().unwrap();
        apk.write_all(&apk_bytes).unwrap();
        assert!(get_apk_signature(apk.path().to_str().unwrap()).is_err());
    }
}
