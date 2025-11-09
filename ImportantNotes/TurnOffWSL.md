Exit WSL first, then shut down
---
| Goal                                  | Command                       | Where to Run     | Effect                     |
| ------------------------------------- | ----------------------------- | ---------------- | -------------------------- |
| Exit current WSL session              | `exit`                        | Inside WSL       | Leaves Linux terminal      |
| Shut down all WSL distros immediately | `wsl --shutdown`              | PowerShell / CMD | Stops everything instantly |
| Shut down from inside WSL             | `cmd.exe /c "wsl --shutdown"` | Inside WSL       | Same as above              |

---

## 💡 What happens when you close Command Prompt or Terminal

| Situation                                                             | Does WSL shut down automatically? | Explanation                                                                                         |
| --------------------------------------------------------------------- | --------------------------------- | --------------------------------------------------------------------------------------------------- |
| ❌ You just **close the Command Prompt / PowerShell / Ubuntu window**  | **No (not immediately)**          | WSL keeps running **in the background** for some time (by default, up to 15 minutes of inactivity). |
| ⏰ WSL becomes **idle (no active process)**                            | **Yes (after a timeout)**         | WSL automatically shuts down after being idle for a while — this saves memory and CPU.              |
| ✅ You run `wsl --shutdown`                                            | **Yes (immediately)**             | All running distros and background services (like Kafka, Zookeeper, etc.) stop right away.          |
| ⚙️ You have a service running (like Kafka, DB, or background process) | **No**                            | WSL stays alive as long as something is running inside it.                                          |

---

## 🧭 To check WSL status

Run this in **PowerShell**:

```powershell
wsl --list --running
```

If nothing shows, it means **WSL is not running**.

---

## 🔌 To manually shut down WSL immediately

Use:

```powershell
wsl --shutdown
```

This ensures all background processes (Kafka, Zookeeper, etc.) are stopped and memory/disk resources are released.

---

## ⚙️ Optional: Change Auto-Shutdown Timeout

By default, WSL shuts down automatically after about **15 minutes** of inactivity.

You can configure it (for example, to auto-shutdown faster) by creating this file on Windows:

```
%UserProfile%\.wslconfig
```

Add this content:

```ini
[wsl2]
memory=4GB
processors=2
localhostForwarding=true
idleTimeout=300
```

Here, `idleTimeout=300` means WSL will auto-shutdown after **5 minutes** of inactivity.

---

### ✅ Best practice

After finishing your Kafka/ZooKeeper or development work:

```powershell
wsl --shutdown
```

That ensures:

* All Linux processes stop
* Disk usage and memory are released
* You start cleanly next time

---
| Step                    | Command                | Where to Run         | Expected Output When Stopped             |
| ----------------------- | ---------------------- | -------------------- | ---------------------------------------- |
| Check running distros   | `wsl --list --running` | PowerShell / CMD     | “There are no running distributions.”    |
| Check overall status    | `wsl --status`         | PowerShell / CMD     | No active distros shown                  |
| Verify via Task Manager | N/A                    | Windows Task Manager | No `vmmem` or `wslservice.exe` processes |
---
Terminate Ubuntu
wsl --terminate Ubuntu
