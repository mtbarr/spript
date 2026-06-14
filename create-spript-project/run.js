#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const projectName = process.argv[2];

if (!projectName) {
  console.error('Please specify the project name:');
  console.error('  npx create-spript-project <project-name>');
  process.exit(1);
}

const projectPath = path.join(process.cwd(), projectName);

if (fs.existsSync(projectPath)) {
  console.error(`The folder ${projectName} already exists. Choose another name or delete it.`);
  process.exit(1);
}

console.log(`\nCreating Spript project at: ${projectPath}...`);

fs.mkdirSync(projectPath, { recursive: true });
fs.mkdirSync(path.join(projectPath, 'src'), { recursive: true });

const packageJson = {
  name: projectName,
  version: "1.0.0",
  description: "TypeScript script for the Spript plugin",
  scripts: {
    "build": "babel src --out-dir dist --extensions \".ts\"",
    "dev": "babel src --out-dir dist --extensions \".ts\" --watch",
    "check": "tsc --noEmit",
    "deploy": "echo \"Add a script to copy the dist/ folder to the server automatically!\""
  },
  dependencies: {},
  devDependencies: {}
};

fs.writeFileSync(
  path.join(projectPath, 'package.json'),
  JSON.stringify(packageJson, null, 2)
);

const tsconfig = {
  compilerOptions: {
    target: "ES6",
    module: "ESNext",
    moduleResolution: "Bundler",
    rootDir: "./src",
    strict: true,
    esModuleInterop: true,
    skipLibCheck: true,
    forceConsistentCasingInFileNames: true,
    noEmit: true,
    typeRoots: ["./node_modules/@grakkit-types", "./node_modules/@types", "./"]
  },
  include: ["src/**/*", "spript-env.d.ts"]
};

fs.writeFileSync(
  path.join(projectPath, 'tsconfig.json'),
  JSON.stringify(tsconfig, null, 2)
);

const babelConfig = `module.exports = {
  presets: [
    ['@babel/preset-env', { targets: "defaults" }],
    '@babel/preset-typescript'
  ],
  plugins: [
    './babel-plugin-java-imports.js'
  ]
};
`;

fs.writeFileSync(
  path.join(projectPath, 'babel.config.js'),
  babelConfig
);

const babelPluginJavaImports = `module.exports = function ({ types: t }) {
  const createJavaTypeCall = (className) =>
    t.callExpression(
      t.identifier('javaType'),
      [t.stringLiteral(className)]
    );

  return {
    visitor: {
      ImportDeclaration(path) {
        const importPath = path.node.source.value;

        const isJavaPackage = /^(org|net|java|com|io)\\./.test(importPath);
        if (!isJavaPackage) return;

        const javaVariables = path.node.specifiers
          .map((specifier) => {
            const localAlias = t.identifier(specifier.local.name);

            if (t.isImportDefaultSpecifier(specifier)) {
              return t.variableDeclarator(localAlias, createJavaTypeCall(importPath));
            }

            if (t.isImportSpecifier(specifier)) {
              const fullClassName = \`\${importPath}.\${specifier.imported.name}\`;
              return t.variableDeclarator(localAlias, createJavaTypeCall(fullClassName));
            }

            return null;
          })
          .filter(Boolean);

        if (javaVariables.length > 0) {
          path.replaceWith(t.variableDeclaration('const', javaVariables));
        } else {
          path.remove();
        }
      },
    },
  };
};
`;

fs.writeFileSync(
  path.join(projectPath, 'babel-plugin-java-imports.js'),
  babelPluginJavaImports
);

const spriptDts = `/// <reference types="@grakkit-types/paper" />
/// <reference types="@grakkit-types/java" />

declare const plugin: org.bukkit.plugin.java.JavaPlugin;
declare const server: org.bukkit.Server;
declare const Bukkit: any;

declare function listen(eventClass: any, callback: (event: any) => void, priority?: org.bukkit.event.EventPriority): void;
declare function command(name: string, callback: (sender: org.bukkit.command.CommandSender, label: string, args: string[]) => void, description?: string, usage?: string, aliases?: string[], tabCompleter?: (sender: org.bukkit.command.CommandSender, alias: string, args: string[]) => string[]): void;
declare function msg(sender: org.bukkit.command.CommandSender | net.kyori.adventure.audience.Audience, text: string): void;
declare function component(text: string): net.kyori.adventure.text.Component;
declare function setTimeout(callback: () => void, delayTicks: number): void;
declare function setInterval(callback: () => void, periodTicks: number): void;
declare function runAsync(callback: () => void): void;
declare function runSync(callback: () => void): void;
declare function createItem(materialName: string, amount?: number, name?: string, loreArray?: string[]): org.bukkit.inventory.ItemStack | null;
declare function javaType<T = any>(className: string): T;

declare const cache: {
    set<T>(key: string, value: T, persistent?: boolean): void;
    get<T>(key: string): T | undefined;
    getOrDefault<T>(key: string, defaultValue: T): T;
    delete(key: string): void;
};

declare const http: {
    get(url: string, headers?: Record<string, string>): Promise<any>;
    post(url: string, body: any, headers?: Record<string, string>): Promise<any>;
};

declare const dotenv: {
    get(key: string, defaultValue?: string): string | null;
    require(key: string): string;
    has(key: string): boolean;
    reload(): void;
};

declare const sql: {
    createPool(config: { url: string; user?: string; password?: string; poolSize?: number }): any;
    query(pool: any, queryStr: string, paramsArray?: any[]): Promise<any[]>;
    execute(pool: any, queryStr: string, paramsArray?: any[]): Promise<number>;
};

type MongoDocument = Record<string, any>;

type RedisPipelineOperation = {
    command: "get" | "set" | "setEx" | "setex" | "delete" | "del" | "exists" | "expire" | "ttl" | "incr" | "decr" | "hGet" | "hget" | "hSet" | "hset" | "hGetAll" | "hgetall" | "lPush" | "lpush" | "rPush" | "rpush" | "lRange" | "lrange" | "publish";
    args: any[];
};

declare const mongo: {
    connect(uri: string): any;
    disconnect(client: any): void;
    database(client: any, databaseName: string): any;
    collection(client: any, databaseName: string, collectionName: string): any;
    find(collection: any, filter?: MongoDocument): Promise<MongoDocument[]>;
    findOne(collection: any, filter?: MongoDocument): Promise<MongoDocument | null>;
    insertOne(collection: any, document: MongoDocument): Promise<string | null>;
    updateOne(collection: any, filter: MongoDocument, update: MongoDocument): Promise<number>;
    deleteOne(collection: any, filter: MongoDocument): Promise<number>;
};

declare const redis: {
    connect(uri: string): any;
    disconnect(client: any): void;
    get(client: any, key: string): Promise<string | null>;
    set(client: any, key: string, value: any): Promise<string>;
    setEx(client: any, key: string, seconds: number, value: any): Promise<string>;
    delete(client: any, keys: string | string[]): Promise<number>;
    exists(client: any, key: string): Promise<boolean>;
    expire(client: any, key: string, seconds: number): Promise<number>;
    ttl(client: any, key: string): Promise<number>;
    incr(client: any, key: string): Promise<number>;
    decr(client: any, key: string): Promise<number>;
    hGet(client: any, key: string, field: string): Promise<string | null>;
    hSet(client: any, key: string, field: string, value: any): Promise<number>;
    hGetAll(client: any, key: string): Promise<Record<string, string>>;
    lPush(client: any, key: string, values: string | string[]): Promise<number>;
    rPush(client: any, key: string, values: string | string[]): Promise<number>;
    lRange(client: any, key: string, start: number, stop: number): Promise<string[]>;
    publish(client: any, channel: string, message: any): Promise<number>;
    pipeline(client: any, operations: RedisPipelineOperation[]): Promise<any[]>;
};
`;

fs.writeFileSync(
  path.join(projectPath, 'spript-env.d.ts'),
  spriptDts
);

const indexTs = `import { PlayerJoinEvent } from 'org.bukkit.event.player';
import { EventPriority } from 'org.bukkit.event';
import { CommandSender } from 'org.bukkit.command';

listen(PlayerJoinEvent, (event: PlayerJoinEvent) => {
    const player = event.getPlayer();
    msg(player, "<green>Welcome to the server running Spript with TypeScript and Babel!");
}, EventPriority.NORMAL);

command("testts", async (sender: CommandSender, label: string, args: string[]) => {
    msg(sender, "<yellow>Buscando seus dados na API da Mojang...");
    
    try {
        const response = await http.get("https://api.mojang.com/users/profiles/minecraft/" + sender.getName());
        const data = JSON.parse(response.body());
        msg(sender, "<green>Seu UUID oficial é: " + data.id);
    } catch (e) {
        msg(sender, "<red>Erro ao buscar dados na Mojang.");
    }
}, "Comando assíncrono de teste TS", "/testts", [], (sender: CommandSender, alias: string, args: string[]) => {
    if (args.length === 1) {
        return ["hello", "world"].filter(s => s.toLowerCase().startsWith(args[0].toLowerCase()));
    }
    return [];
});
`;

fs.writeFileSync(
  path.join(projectPath, 'src', 'index.ts'),
  indexTs
);

fs.writeFileSync(
  path.join(projectPath, '.gitignore'),
  "node_modules\ndist\n.env\n"
);

console.log('Installing Babel, TypeScript, and Autocomplete dependencies...');
try {
  const dependencies = [
    'typescript',
    '@babel/core',
    '@babel/cli',
    '@babel/preset-env',
    '@babel/preset-typescript',
    '@grakkit-types/paper@1.20.2-1.0.0',
    '@grakkit-types/java'
  ].join(' ');

  execSync(`npm install --save-dev ${dependencies}`, {
    cwd: projectPath,
    stdio: 'inherit'
  });

  console.log('\nProject created successfully!');
  console.log('\nStart coding by typing:');
  console.log(`  cd ${projectName}`);
  console.log(`  code .`);
  console.log('\nTo compile the code, use:');
  console.log(`  npm run build`);
  console.log('  (or "npm run dev" to compile automatically on save)');
} catch (error) {
  console.error('\nError installing dependencies. Make sure you have Node.js and NPM installed.');
}
