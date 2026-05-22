#!/usr/bin/env node
/**
 * Agent Coordinator hook - run before starting antigravity agent.
 * Run: node .agent-coordinator/hooks/antigravity.js
 */
import { execSync } from 'child_process';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function findProjectRoot() {
  let dir = path.resolve(__dirname, '../..');
  for (let i = 0; i < 10; i++) {
    try {
      if (fs.existsSync(path.join(dir, '.agent-coordinator'))) return dir;
    } catch (_) {}
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return process.cwd();
}

const root = findProjectRoot();
execSync(`npx coordinator context inject antigravity`, { stdio: 'inherit', cwd: root });
