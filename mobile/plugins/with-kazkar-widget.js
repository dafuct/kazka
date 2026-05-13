const { withEntitlementsPlist, withXcodeProject } = require('@expo/config-plugins');
const fs = require('fs');
const path = require('path');

const WIDGET_TARGET_NAME = 'KazkarWidget';
const APP_GROUP_ID = 'group.app.kazka';

/**
 * Adds:
 *  - App Group entitlement to the main app
 *  - Copies Swift sources from <repo>/mobile/widget/* into ios/KazkarWidget/
 *
 * The actual Xcode target (PBXNativeTarget for the widget extension) must be
 * added once manually in Xcode — subsequent prebuilds preserve it since
 * mobile/ios/ is committed.
 */
const withKazkarWidget = (config) => {
  config = withEntitlementsPlist(config, (cfg) => {
    const existing = cfg.modResults['com.apple.security.application-groups'];
    const groups = Array.isArray(existing) ? existing : [];
    if (!groups.includes(APP_GROUP_ID)) groups.push(APP_GROUP_ID);
    cfg.modResults['com.apple.security.application-groups'] = groups;
    return cfg;
  });

  config = withXcodeProject(config, async (cfg) => {
    const projectRoot = cfg.modRequest.projectRoot;
    const iosRoot = cfg.modRequest.platformProjectRoot;
    const targetDir = path.join(iosRoot, WIDGET_TARGET_NAME);

    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    const widgetSrcDir = path.join(projectRoot, 'widget');
    if (fs.existsSync(widgetSrcDir)) {
      copyRecursive(widgetSrcDir, targetDir);
      console.log(`[with-kazkar-widget] Copied ${widgetSrcDir} → ${targetDir}`);
    } else {
      console.warn(`[with-kazkar-widget] Source dir ${widgetSrcDir} not found — skipping copy`);
    }

    console.warn(
      '[with-kazkar-widget] Files in place. Add the KazkarWidget extension target manually ' +
      'in Xcode the first time (File → New → Target → Widget Extension), then commit ios/.'
    );
    return cfg;
  });

  return config;
};

function copyRecursive(src, dst) {
  if (!fs.existsSync(dst)) fs.mkdirSync(dst, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, entry.name);
    const d = path.join(dst, entry.name);
    if (entry.isDirectory()) copyRecursive(s, d);
    else fs.copyFileSync(s, d);
  }
}

module.exports = withKazkarWidget;
