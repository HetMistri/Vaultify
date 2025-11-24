import express from "express";
import cors from "cors";
import morgan from "morgan";
import bodyParser from "body-parser";
import routes from "./routes/index.js";

const app = express();
const PORT = process.env.PORT || 3000;

// ============================================
// MIDDLEWARE
// ============================================

// Enable CORS for all origins
app.use(cors());

// HTTP request logger
app.use(morgan("dev"));

// Parse JSON bodies
app.use(bodyParser.json());

// Parse URL-encoded bodies
app.use(bodyParser.urlencoded({ extended: true }));

// ============================================
// ROUTES
// ============================================

// API routes
app.use("/api", routes);

// Root endpoint
app.get("/", (req, res) => {
  res.json({
    service: "Vaultify Ledger Server",
    version: "1.0.0",
    description: "Public Certificate Registry & Audit Ledger",
    endpoints: {
      health: "/api/health",
      ledger: "/api/ledger/blocks",
      certificates: "/api/certificates",
      tokens: "/api/tokens/revoked",
      publicKeys: "/api/users/:userId/public-key",
    },
    documentation: "See README.md for full API documentation",
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: "Not Found",
    message: `Cannot ${req.method} ${req.url}`,
  });
});

// Error handler
app.use((err, req, res, next) => {
  console.error("Error:", err);
  res.status(err.status || 500).json({
    error: err.message || "Internal Server Error",
  });
});

// ============================================
// START SERVER
// ============================================

app.listen(PORT, () => {
  console.log("═══════════════════════════════════════════════════════");
  console.log("  🔐 VAULTIFY LEDGER SERVER");
  console.log("═══════════════════════════════════════════════════════");
  console.log(`  Server running on: http://localhost:${PORT}`);
  console.log(`  Environment: ${process.env.NODE_ENV || "development"}`);
  console.log("═══════════════════════════════════════════════════════");
  console.log("  Endpoints:");
  console.log(`    • Health:        http://localhost:${PORT}/api/health`);
  console.log(
    `    • Ledger:        http://localhost:${PORT}/api/ledger/blocks`
  );
  console.log(`    • Certificates:  http://localhost:${PORT}/api/certificates`);
  console.log(
    `    • Tokens:        http://localhost:${PORT}/api/tokens/revoked`
  );
  console.log(
    `    • Public Keys:   http://localhost:${PORT}/api/users/:userId/public-key`
  );
  console.log("═══════════════════════════════════════════════════════");
});

export default app;
