#!/bin/bash

tsc --out res/admin/webroot/lib/pagegraph.js -t ES5 src/typescript/pagegraph.ts
tsc --out res/admin/webroot/js/admin.js -t ES5 src/typescript/admin.ts
