#!/bin/bash
set -e
tsc --out res/admin/webroot/lib/pagegraph.js -t ES5 src/typescript/web/pagegraph.ts
tsc --out res/admin/webroot/js/admin.js -t ES5 src/typescript/web/admin.ts
