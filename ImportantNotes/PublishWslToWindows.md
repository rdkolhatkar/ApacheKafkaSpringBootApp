
---

# Make WSL (Ubuntu) accessible from Windows — step-by-step

> Assumes **WSL2** (not WSL1). Commands run in WSL terminal unless prefixed with `C:\>` meaning Windows cmd/PowerShell.

---

## 1) Find your WSL IP

In WSL (Ubuntu) run:

```bash
hostname -I
```

Example output:

```
172.30.177.3
```

> This is the WSL2 VM IP that Windows can reach.

---

## 2) Pick the routable IP to advertise (WSL IP)

Use that IP (e.g. `172.30.177.3`) as the value you **advertise** to clients (Windows) — Kafka requires a routable IP (not `0.0.0.0`).

---

## 3) Edit Kafka server config (KRaft mode) to use the WSL IP

Open your Kafka `server.properties` (the file you pasted previously). Update the `listeners` and `advertised.listeners` lines so broker advertises the WSL IP:

Example edits:

```properties
# bind to all interfaces on the WSL VM
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093

# advertise WSL's routable IP (use the IP from step 1)
advertised.listeners=PLAINTEXT://172.30.177.3:9092,CONTROLLER://172.30.177.3:9093

# controller quorum voters if using KRaft
controller.quorum.voters=1@172.30.177.3:9093
```

Notes:

* `listeners` binds (0.0.0.0 is OK to listen on all interfaces).
* `advertised.listeners` **must** be a routable IP (WSL IP).
* Also use the same WSL IP in `controller.quorum.voters` (KRaft).

Save changes.

---

## 4) If you prefer using host name instead of IP

You can use your Windows machine IP reachable from WSL (or a DNS name), but advertise a routable address that Windows clients can reach. Usually easier: advertise WSL IP.

---

## 5) Ensure WSL firewall (Ubuntu) allows broker ports

In WSL (if `ufw` is enabled), open ports 9092 and 9093, or disable ufw during testing:

To allow ports:

```bash
sudo ufw allow 9092/tcp
sudo ufw allow 9093/tcp
# check status
sudo ufw status
```

Or temporarily:

```bash
sudo ufw disable
```

(Only disable if you understand the implications.)

---

## 6) Allow Windows Firewall inbound rule for the WSL ports

On Windows (run as Administrator PowerShell):

PowerShell:

```powershell
New-NetFirewallRule -DisplayName "Allow Kafka 9092 from WSL" -Direction Inbound -LocalPort 9092 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "Allow Kafka 9093 from WSL" -Direction Inbound -LocalPort 9093 -Protocol TCP -Action Allow
```

Or CMD:

```cmd
netsh advfirewall firewall add rule name="Allow Kafka 9092" dir=in action=allow protocol=TCP localport=9092
netsh advfirewall firewall add rule name="Allow Kafka 9093" dir=in action=allow protocol=TCP localport=9093
```

(This lets Windows apps connect to WSL broker IP/ports.)

---

## 7) Restart Kafka in WSL

Stop and start Kafka using its scripts inside WSL (KRaft example):

```bash
# stop (if running)
pkill -f kafka || true

# start broker
# adapt path to your kafka folder
~/kafka_2.13-4.1/bin/kafka-server-start.sh ~/kafka_2.13-4.1/config/kraft/server.properties
```

Watch logs for startup success and the broker advertising the IP.

---

## 8) Verify Kafka is listening on the WSL IP (in WSL)

In WSL:

```bash
ss -ltnp | grep 9092
# or
netstat -tlnp | grep 9092
```

You should see it listening `0.0.0.0:9092` (bound) — but advertised is what clients use.

---

## 9) Test connectivity from Windows (client side)

From Windows CMD/Powershell:

### Option A — Use Kafka tools on Windows

If you have Kafka bin scripts on Windows, run:

```cmd
# from kafka/bin/windows
kafka-topics.bat --bootstrap-server 172.30.177.3:9092 --list
```

### Option B — Use `telnet`/`Test-NetConnection` to check port

PowerShell:

```powershell
Test-NetConnection -ComputerName 172.30.177.3 -Port 9092
```

Should show `TcpTestSucceeded : True`.

### Option C — Using `kcat` / `nc` — test produce/consume

If you have `kcat` (formerly kafkacat) or Kafka client, try producing or listing topics.

---

## 10) If connection still times out — troubleshooting checklist

* Re-check the WSL IP: `hostname -I`
* Confirm `advertised.listeners` uses exact WSL IP and matching ports.
* Confirm Kafka started without fatal exceptions (no "advertised.listeners cannot use 0.0.0.0" and no "TimeoutException" in logs).
* Confirm Windows firewall rules were created successfully.
* Confirm WSL’s `ufw` or other Linux firewall allowed ports.
* Confirm no other Windows process is blocking the port.

---

## 11) Make WSL IP handling robust (WSL IP changes on reboot)

WSL2 IP changes when the VM restarts. Options:

### Option A — On every WSL start, update server.properties automatically

Create a small script in WSL that replaces placeholders in `server.properties` with current IP and restarts Kafka.

Example `start-kafka-with-ip.sh`:

```bash
#!/usr/bin/env bash
KAFKA_DIR=~/kafka_2.13-4.1
CONF=$KAFKA_DIR/config/kraft/server.properties
IP=$(hostname -I | awk '{print $1}')
# replace placeholders or set lines
sed -i "s/^advertised.listeners=.*/advertised.listeners=PLAINTEXT:\/\/${IP}:9092,CONTROLLER:\/\/${IP}:9093/" $CONF
sed -i "s/^controller.quorum.voters=.*/controller.quorum.voters=1@${IP}:9093/" $CONF

# then start kafka
$KAFKA_DIR/bin/kafka-server-start.sh $CONF
```

Make executable: `chmod +x start-kafka-with-ip.sh` and run it to start Kafka with the current IP.

### Option B — Use Windows host to reach WSL via the special WSL localhost forwarding

Recent Windows versions forward services running on WSL to `localhost` on Windows automatically for processes listening on `0.0.0.0`. But advertised.listeners must be routable — so if you want to use `localhost` from Windows, you can instead run Kafka in WSL with `advertised.listeners=PLAINTEXT://127.0.0.1:9092` **only** if Windows can reach that same loopback (this is tricky) — generally safer to advertise WSL IP.

### Option C — Create a Windows startup scheduled task that retrieves WSL IP and updates a hosts/forward mapping — advanced.

---

## 12) Example summary of minimal working configuration (server.properties)

```properties
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@172.30.177.3:9093

listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=PLAINTEXT://172.30.177.3:9092,CONTROLLER://172.30.177.3:9093

log.dirs=/tmp/kraft-combined-logs
num.partitions=1

# replication/isr appropriate for dev
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
```

Replace `172.30.177.3` with your `hostname -I` result.

---

## 13) Final sanity-check steps (quick list)

1. In WSL: `hostname -I` → copy IP.
2. Edit Kafka config `advertised.listeners` and `controller.quorum.voters` to that IP.
3. Allow ports on WSL (`ufw`) and on Windows (create firewall rules).
4. Start Kafka (`kafka-server-start.sh server.properties`).
5. From Windows: `kafka-topics.bat --bootstrap-server <WSL_IP>:9092 --list` → should show topics (or empty).
6. If errors repeat, paste broker logs — I can read them and point exact fix.

---

## 14) Quick common pitfalls (and fixes)

* **Error:** `advertised.listeners cannot use the nonroutable meta-address 0.0.0.0` → Fix: set advertised.listeners to actual WSL IP (not `0.0.0.0`).
* **Error:** `Timed out waiting for a node assignment` → broker not reachable; check IP, firewall, Kafka startup errors.
* **Broker repeatedly exits in KRaft mode** → ensure controller.quorum.voters uses the same reachable IP and port; check logs for Raft/controller errors.
* **WSL IP changes after reboot** → use the script (Step 11 Option A) to update config and restart Kafka automatically.

---