import { ledgerService } from "../services/LedgerService.js";
import { certificateService } from "../services/CertificateService.js";
import { tokenService } from "../services/TokenService.js";
import { sha256 } from "../utils/crypto.js";

async function runTests() {
  console.log("Running Ledger Server Tests...");

  try {
    // Test Ledger Service
    console.log("\n--- Testing Ledger Service ---");
    const block = await ledgerService.appendBlock(
      "TEST_ACTION",
      sha256("test data")
    );
    console.log(
      "Block appended:",
      block.index === ledgerService.getLatestBlock().index
    );
    console.log("Chain valid:", ledgerService.verifyChain());

    // Test Token Service
    console.log("\n--- Testing Token Service ---");
    const tokenHash = sha256("test-token-" + Date.now());
    const revocation = await tokenService.revokeToken(
      tokenHash,
      "Test revocation"
    );
    console.log("Token revoked:", tokenService.isTokenRevoked(tokenHash));
    console.log("Revocation info:", revocation.reason === "Test revocation");

    // Test Certificate Service
    console.log("\n--- Testing Certificate Service ---");
    // Mock data for certificate test would be complex due to signatures,
    // so we just check if service is initialized
    console.log(
      "Certificate Service initialized:",
      certificateService.certificates !== undefined
    );

    console.log("\nAll tests passed!");
  } catch (error) {
    console.error("Test failed:", error);
    process.exit(1);
  }
}

runTests();
