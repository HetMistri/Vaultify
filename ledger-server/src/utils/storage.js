import fs from "fs";
import fsPromises from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DATA_DIR = path.join(__dirname, "../../data");

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

/**
 * Read JSON file
 * @param {string} filename - Name of the file (e.g., 'ledger.json')
 * @param {any} defaultValue - Default value if file doesn't exist
 * @returns {any} Parsed JSON data
 */
export function readJSON(filename, defaultValue = null) {
  const filepath = path.join(DATA_DIR, filename);

  if (!fs.existsSync(filepath)) {
    return defaultValue;
  }

  try {
    const data = fs.readFileSync(filepath, "utf8");
    return JSON.parse(data);
  } catch (error) {
    console.error(`Error reading ${filename}:`, error.message);
    return defaultValue;
  }
}

/**
 * Write JSON file (Synchronous)
 * @param {string} filename - Name of the file (e.g., 'ledger.json')
 * @param {any} data - Data to write
 * @returns {boolean} Success status
 */
export function writeJSON(filename, data) {
  const filepath = path.join(DATA_DIR, filename);

  try {
    fs.writeFileSync(filepath, JSON.stringify(data, null, 2), "utf8");
    return true;
  } catch (error) {
    console.error(`Error writing ${filename}:`, error.message);
    return false;
  }
}

/**
 * Write JSON file (Asynchronous)
 * @param {string} filename - Name of the file (e.g., 'ledger.json')
 * @param {any} data - Data to write
 * @returns {Promise<boolean>} Success status
 */
export async function writeJSONAsync(filename, data) {
  const filepath = path.join(DATA_DIR, filename);

  try {
    await fsPromises.writeFile(filepath, JSON.stringify(data, null, 2), "utf8");
    return true;
  } catch (error) {
    console.error(`Error writing ${filename}:`, error.message);
    return false;
  }
}

/**
 * Append to JSON array file
 * @param {string} filename - Name of the file
 * @param {any} item - Item to append
 * @returns {boolean} Success status
 */
export function appendToJSON(filename, item) {
  const data = readJSON(filename, []);
  data.push(item);
  return writeJSON(filename, data);
}

/**
 * Check if file exists
 * @param {string} filename - Name of the file
 * @returns {boolean} True if file exists
 */
export function fileExists(filename) {
  const filepath = path.join(DATA_DIR, filename);
  return fs.existsSync(filepath);
}
