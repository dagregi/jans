# 🚀 Quick Start Guide

## 3-Minute Setup and Validation

### Prerequisites Check

```bash
java -version  # Should show Java 11+
mvn -version   # Should show Maven 3.6+
```

### Step 1: Start the Server (30 seconds)

```bash
cd /path/to/jans/jans-federation-vibe
./deployment/scripts/start.sh
```

**Expected Output:**
```
✅ Jans Federation Vibe Started!
🌐 Application URL: http://localhost:8080
```

### Step 2: Verify It's Running (5 seconds)

```bash
./deployment/scripts/status.sh
```

**Expected Output:**
```
Status: ✅ RUNNING
All endpoints: ✅ Healthy
```

### Step 3: Run Integration Tests (2 seconds)

```bash
mvn test
```

**Expected Output:**
```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Step 4: Test the API (10 seconds)

```bash
# Get federation metadata
curl http://localhost:8080/federation/metadata | jq '.'

# Get entity configuration
curl "http://localhost:8080/.well-known/openid-federation?iss=https://op.example.com" | jq '.'

# Get trust marks
curl http://localhost:8080/federation/trust-marks | jq '.'
```

### Step 5: Stop the Server (2 seconds)

```bash
./deployment/scripts/stop.sh
```

**Expected Output:**
```
✅ Jans Federation Vibe Stopped
```

---

## 🎉 You're Done!

You've just:
- ✅ Started an OpenID Federation 1.0 server
- ✅ Validated all endpoints are working
- ✅ Run 10 comprehensive integration tests
- ✅ Tested the API manually
- ✅ Stopped the server gracefully

---

## 📖 Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Review [TEST_VALIDATION_REPORT.md](TEST_VALIDATION_REPORT.md) for test details
- Check [VERIFICATION.md](VERIFICATION.md) for complete validation results
- Explore the [OpenID Federation 1.0 specification](https://openid.net/specs/openid-federation-1_0.html)

---

## 🆘 Troubleshooting

**Server won't start?**
```bash
# Check logs
tail -f /tmp/federation-server.log

# Check if port 8080 is in use
lsof -i :8080
```

**Tests failing?**
```bash
# Make sure server is running
./deployment/scripts/status.sh

# Should show: Status: ✅ RUNNING
```

**Need help?**
- Check the comprehensive [README.md](README.md)
- Review the [VERIFICATION.md](VERIFICATION.md) for troubleshooting steps


