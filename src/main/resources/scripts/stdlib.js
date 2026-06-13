var stdlib = (function() {
    var Material = Java.type('org.bukkit.Material');
    var ItemStack = Java.type('org.bukkit.inventory.ItemStack');
    var ArrayList = Java.type('java.util.ArrayList');

    function awaitFuture(future) {
        return new Promise(function(resolve, reject) {
            future.whenComplete(function(result, error) {
                // Força o retorno do Promise a rodar na Main-Thread do Bukkit
                api.runTask(function() {
                    if (error) {
                        reject(error);
                    } else {
                        resolve(result);
                    }
                });
            });
        });
    }

    return {
        listen: function(eventClass, callback, priority) {
            api.registerListener(eventClass, callback, priority || "NORMAL");
        },

        command: function(name, callback, description, usage, aliases, tabCompleter) {
            api.registerCommand(name, callback, description || "", usage || ("/" + name), aliases || [], tabCompleter || null);
        },

        msg: function(sender, text) {
            api.sendMessage(sender, text);
        },

        component: function(text) {
            return api.component(text);
        },

        setTimeout: function(callback, delayTicks) {
            api.runTaskLater(callback, delayTicks);
        },

        setInterval: function(callback, periodTicks) {
            api.runTaskTimer(callback, 0, periodTicks);
        },

        runAsync: function(callback) {
            api.runTaskAsync(callback);
        },

        runSync: function(callback) {
            api.runTask(callback);
        },

        createItem: function(materialName, amount, name, loreArray) {
            var mat = Material.matchMaterial(materialName);
            if (!mat) {
                plugin.getLogger().warning("Material inválido fornecido ao createItem: " + materialName);
                return null;
            }
            
            var item = new ItemStack(mat, amount || 1);
            var meta = item.getItemMeta();
            if (meta) {
                if (name) {
                    meta.displayName(api.component(name));
                }
                if (loreArray && loreArray.length > 0) {
                    var loreList = new ArrayList();
                    for (var i = 0; i < loreArray.length; i++) {
                        loreList.add(api.component(loreArray[i]));
                    }
                    meta.lore(loreList);
                }
                item.setItemMeta(meta);
            }
            return item;
        },

        cache: {
            set: function(key, value, persistent) {
                $cache.set(key, value, persistent || false);
            },
            get: function(key) {
                return $cache.get(key);
            },
            getOrDefault: function(key, defaultValue) {
                return $cache.getOrDefault(key, defaultValue);
            },
            delete: function(key) {
                $cache.delete(key);
            }
        },

        http: {
            get: function(url, headers) {
                return awaitFuture($http.get(url, headers || null));
            },
            post: function(url, body, headers) {
                var bodyStr = typeof body === 'string' ? body : JSON.stringify(body);
                return awaitFuture($http.post(url, bodyStr, headers || null));
            }
        },

        sql: {
            createPool: function(config) {
                return $sql.createPool(config);
            },
            query: function(pool, queryStr, paramsArray) {
                var ListType = Java.type("java.util.ArrayList");
                var params = null;
                if (paramsArray) {
                    params = new ListType();
                    for (var i = 0; i < paramsArray.length; i++) {
                        params.add(paramsArray[i]);
                    }
                }
                return awaitFuture($sql.query(pool, queryStr, params)).then(function(resultList) {
                    var arr = [];
                    for (var i = 0; i < resultList.size(); i++) {
                        var map = resultList.get(i);
                        var obj = {};
                        var iterator = map.keySet().iterator();
                        while (iterator.hasNext()) {
                            var key = iterator.next();
                            obj[key] = map.get(key);
                        }
                        arr.push(obj);
                    }
                    return arr;
                });
            },
            execute: function(pool, queryStr, paramsArray) {
                var ListType = Java.type("java.util.ArrayList");
                var params = null;
                if (paramsArray) {
                    params = new ListType();
                    for (var i = 0; i < paramsArray.length; i++) {
                        params.add(paramsArray[i]);
                    }
                }
                return awaitFuture($sql.execute(pool, queryStr, params));
            }
        }
    };
})();

var listen = stdlib.listen;
var command = stdlib.command;
var msg = stdlib.msg;
var component = stdlib.component;
var setTimeout = stdlib.setTimeout;
var setInterval = stdlib.setInterval;
var runAsync = stdlib.runAsync;
var runSync = stdlib.runSync;
var createItem = stdlib.createItem;
var cache = stdlib.cache;
var http = stdlib.http;
var sql = stdlib.sql;

exports = stdlib;
