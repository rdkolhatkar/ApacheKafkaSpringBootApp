
---

## 🧹 WSL Cleanup Overview

| Cleanup Type                                        | Command                                                   | What It Does                                                        | Safe to Use?                   |                 |
| --------------------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------------- | ------------------------------ | --------------- |
| 🗑️ Clear APT package cache                         | `sudo apt clean`                                          | Removes cached `.deb` files downloaded during updates or installs.  | ✅ Safe                         |                 |
| 🧰 Remove old unused packages                       | `sudo apt autoremove --purge -y`                          | Deletes old, unused dependencies (e.g., from uninstalled software). | ✅ Safe                         |                 |
| 📦 Clear thumbnail, cache & temp files              | `sudo rm -rf ~/.cache/* /tmp/*`                           | Deletes user & system temporary files.                              | ✅ Safe                         |                 |
| 🧹 Clear system logs                                | `sudo journalctl --vacuum-time=3d`                        | Keeps only logs from the last 3 days.                               | ✅ Safe                         |                 |
| 🧽 Clear npm/yarn caches                            | `npm cache clean --force` / `yarn cache clean`            | Removes JS package manager cache (if installed).                    | ✅ Safe                         |                 |
| 🧰 Clear Docker images/containers (if using Docker) | `docker system prune -a`                                  | Deletes stopped containers, dangling images, and caches.            | ⚠️ Only if using Docker        |                 |
| 🧩 Remove orphaned Snap packages                    | `sudo snap list --all` → `sudo snap remove <old-version>` | Removes outdated Snap app versions.                                 | ⚠️ Only if using Snaps         |                 |
| 🧾 Check disk usage                                 | `df -h` or `du -sh ~/*                                    | sort -h`                                                            | Shows disk usage by directory. | ✅ Informational |

---

## 🧠 Step-by-Step Cleanup Commands

### 1️⃣ Clean apt package cache

```bash
sudo apt clean
sudo apt autoclean
```

### 2️⃣ Remove unused dependencies

```bash
sudo apt autoremove --purge -y
```

### 3️⃣ Delete user & system cache/temp

```bash
sudo rm -rf ~/.cache/*
sudo rm -rf /tmp/*
```

### 4️⃣ Trim journal logs (system logs)

```bash
sudo journalctl --vacuum-time=3d
```

👉 Keeps only 3 days of logs; change `3d` to `1d` or `1h` if needed.

### 5️⃣ (Optional) Clean npm/yarn caches

```bash
npm cache clean --force
yarn cache clean
```

### 6️⃣ (Optional) Clean Docker space

If you’ve been experimenting with Docker inside WSL:

```bash
docker system prune -a
```

⚠️ Removes *all stopped containers, unused images, and build cache*.

### 7️⃣ See what’s using space

```bash
sudo du -h --max-depth=1 / | sort -h
```

or for your home folder:

```bash
du -sh ~/* | sort -h
```

---

## 🧰 Bonus: Reclaim disk space on Windows side (WSL virtual disk)

If you’ve cleaned inside WSL but Windows still shows high usage, compact the WSL virtual disk:

### 🔹 Step 1: Shut down WSL

```bash
wsl --shutdown
```

### 🔹 Step 2: Open PowerShell as Administrator

Then run:

```powershell
wsl --manage <distroName> --compact
```

Example:

```powershell
wsl --list
wsl --manage Ubuntu --compact
```

This compacts the WSL virtual disk (shrinks the `.vhdx` file).
---

### 🧩 Example of Distro Names

When you run this command in **PowerShell or Command Prompt**:

```powershell
wsl --list --verbose
```

or the short version:

```powershell
wsl -l -v
```

You’ll see something like this:

```
  NAME                   STATE           VERSION
* Ubuntu                 Running         2
  docker-desktop         Stopped         2
  docker-desktop-data    Stopped         2
```

In this case:

| Distro Name             | Description                                                      |
| ----------------------- | ---------------------------------------------------------------- |
| **Ubuntu**              | Your main Linux distribution (this is what you use in terminal). |
| **docker-desktop**      | Used internally by Docker Desktop on Windows.                    |
| **docker-desktop-data** | Stores Docker data.                                              |

---

### 💡 So:

* Your **`distroName`** = the **NAME** shown in the first column.
* For example, if you’re using **Ubuntu**, the command to compact your WSL disk would be:

  ```powershell
  wsl --manage Ubuntu --compact
  ```

---

### ✅ Summary

| Command                               | Purpose                                                        |
| ------------------------------------- | -------------------------------------------------------------- |
| `wsl --list --verbose`                | Shows all installed WSL distros and their names                |
| `wsl --shutdown`                      | Stops all running WSL instances                                |
| `wsl --manage <distroName> --compact` | Compacts/shrinks the WSL virtual disk for the specified distro |

---
---

## 💡 Tip

You can automate cleanup by adding a small alias:

```bash
echo "alias wslclean='sudo apt clean && sudo apt autoremove -y && sudo rm -rf ~/.cache/* /tmp/* && sudo journalctl --vacuum-time=3d'" >> ~/.bashrc
source ~/.bashrc
```

Then simply run:

```bash
wslclean
```

---