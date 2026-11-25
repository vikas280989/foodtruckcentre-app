#!/usr/bin/env bash
# Sets up a throw-away local WordPress (SQLite) with the FTC App API plugin, for automated tests.
# Usage: ci/setup_local_wp.sh <work_dir>    Then serve with: php -S 0.0.0.0:8800 <work_dir>/router.php
set -euo pipefail
WORK="$1"; REPO="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$WORK" && cd "$WORK"
curl -sSL -o wp-cli.phar https://github.com/wp-cli/wp-cli/releases/download/v2.12.0/wp-cli-2.12.0.phar
WP="php $WORK/wp-cli.phar --allow-root --path=$WORK/wp"
git clone -q --depth 1 --branch 6.8.3 https://github.com/WordPress/WordPress wp
git clone -q --depth 1 https://github.com/WordPress/sqlite-database-integration sqlite
P=wp/wp-content/plugins/sqlite-database-integration
cp -rL sqlite/packages/plugin-sqlite-database-integration "$P"
sed -e "s#{SQLITE_IMPLEMENTATION_FOLDER_PATH}#$WORK/$P#" -e "s#{SQLITE_PLUGIN}#sqlite-database-integration/load.php#" "$P/db.copy" > wp/wp-content/db.php
$WP config create --dbname=wp --dbuser=x --dbpass=x --dbhost=localhost --skip-check --quiet
$WP config set FTC_APP_ALLOW_HTTP_FOR_TESTS true --raw
$WP core install --url=http://127.0.0.1:8800 --title=FTC --admin_user=admin --admin_password="$(openssl rand -hex 12)" --admin_email=admin@example.test --skip-email
$WP option update home http://127.0.0.1:8800
$WP option update siteurl http://127.0.0.1:8800
$WP rewrite structure '/%postname%/'
cp -r "$REPO/wordpress-plugin/ftc-app-api" wp/wp-content/plugins/
mkdir -p wp/wp-content/mu-plugins outbox
cat > wp/wp-content/mu-plugins/test-mail.php <<PHP
<?php
// TEST ONLY: capture wp_mail into $WORK/outbox instead of sending. A file named FAIL makes sending fail.
add_filter('pre_wp_mail', function (\$null, \$atts) {
    \$dir = '$WORK/outbox';
    if (file_exists("\$dir/FAIL")) { do_action('wp_mail_failed', new WP_Error('wp_mail_failed', 'SMTP connect() failed (simulated)')); return false; }
    file_put_contents("\$dir/" . microtime(true) . '-' . wp_rand() . '.json', wp_json_encode(\$atts));
    return true;
}, 10, 2);
PHP
$WP plugin activate ftc-app-api
$WP rewrite flush
cat > router.php <<'PHP'
<?php
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$file = __DIR__ . '/wp' . $path;
if ($path !== '/' && is_file($file) && !str_ends_with($file, '.php')) return false;
if ($path !== '/' && is_file($file)) { chdir(dirname($file)); require $file; return true; }
$_SERVER['SCRIPT_NAME'] = '/index.php';
chdir(__DIR__ . '/wp'); require __DIR__ . '/wp/index.php';
PHP
echo "Local WordPress ready in $WORK"
