var stdlib = (function() {
    var Material = javaType('org.bukkit.Material');
    var ItemStack = javaType('org.bukkit.inventory.ItemStack');
    var ArrayList = javaType('java.util.ArrayList');
    var JavaMap = javaType('java.util.Map');
    var JavaList = javaType('java.util.List');

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

    function toJs(value) {
        if (value === null || value === undefined) return value;

        if (value instanceof JavaMap) {
            var obj = {};
            var keys = value.keySet().iterator();
            while (keys.hasNext()) {
                var key = keys.next();
                obj[key] = toJs(value.get(key));
            }
            return obj;
        }

        if (value instanceof JavaList) {
            var arr = [];
            for (var i = 0; i < value.size(); i++) {
                arr.push(toJs(value.get(i)));
            }
            return arr;
        }

        return value;
    }

    return {
        listen: function(eventClass, callback, priority) {
            api.registerListener(eventClass, callback, priority || "NORMAL");
        },

        javaType: javaType,

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

        dotenv: {
            get: function(key, defaultValue) {
                return $dotenv.get(key, defaultValue === undefined ? null : defaultValue);
            },
            require: function(key) {
                return $dotenv.require(key);
            },
            has: function(key) {
                return $dotenv.has(key);
            },
            reload: function() {
                $dotenv.reload();
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
                var ListType = javaType("java.util.ArrayList");
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
                var ListType = javaType("java.util.ArrayList");
                var params = null;
                if (paramsArray) {
                    params = new ListType();
                    for (var i = 0; i < paramsArray.length; i++) {
                        params.add(paramsArray[i]);
                    }
                }
                return awaitFuture($sql.execute(pool, queryStr, params));
            }
        },

        mongo: {
            connect: function(uri) {
                return $mongo.connect(uri);
            },
            disconnect: function(client) {
                $mongo.disconnect(client);
            },
            database: function(client, databaseName) {
                return $mongo.database(client, databaseName);
            },
            collection: function(client, databaseName, collectionName) {
                return $mongo.collection(client, databaseName, collectionName);
            },
            find: function(collection, filter) {
                return awaitFuture($mongo.find(collection, filter || null)).then(toJs);
            },
            findOne: function(collection, filter) {
                return awaitFuture($mongo.findOne(collection, filter || null)).then(toJs);
            },
            insertOne: function(collection, document) {
                return awaitFuture($mongo.insertOne(collection, document));
            },
            updateOne: function(collection, filter, update) {
                return awaitFuture($mongo.updateOne(collection, filter, update));
            },
            deleteOne: function(collection, filter) {
                return awaitFuture($mongo.deleteOne(collection, filter));
            }
        },

        redis: {
            connect: function(uri) {
                return $redis.connect(uri);
            },
            disconnect: function(client) {
                $redis.disconnect(client);
            },
            get: function(client, key) {
                return awaitFuture($redis.get(client, key));
            },
            set: function(client, key, value) {
                return awaitFuture($redis.set(client, key, String(value)));
            },
            setEx: function(client, key, seconds, value) {
                return awaitFuture($redis.setEx(client, key, seconds, String(value)));
            },
            delete: function(client, keys) {
                return awaitFuture($redis.delete(client, keys));
            },
            exists: function(client, key) {
                return awaitFuture($redis.exists(client, key));
            },
            expire: function(client, key, seconds) {
                return awaitFuture($redis.expire(client, key, seconds));
            },
            ttl: function(client, key) {
                return awaitFuture($redis.ttl(client, key));
            },
            incr: function(client, key) {
                return awaitFuture($redis.incr(client, key));
            },
            decr: function(client, key) {
                return awaitFuture($redis.decr(client, key));
            },
            hGet: function(client, key, field) {
                return awaitFuture($redis.hGet(client, key, field));
            },
            hSet: function(client, key, field, value) {
                return awaitFuture($redis.hSet(client, key, field, String(value)));
            },
            hGetAll: function(client, key) {
                return awaitFuture($redis.hGetAll(client, key)).then(toJs);
            },
            lPush: function(client, key, values) {
                return awaitFuture($redis.lPush(client, key, values));
            },
            rPush: function(client, key, values) {
                return awaitFuture($redis.rPush(client, key, values));
            },
            lRange: function(client, key, start, stop) {
                return awaitFuture($redis.lRange(client, key, start, stop)).then(toJs);
            },
            publish: function(client, channel, message) {
                return awaitFuture($redis.publish(client, channel, String(message)));
            },
            pipeline: function(client, operations) {
                return awaitFuture($redis.pipeline(client, operations)).then(toJs);
            }
        }
    };
})();

var listen = stdlib.listen;
var javaType = stdlib.javaType;
var command = stdlib.command;
var msg = stdlib.msg;
var component = stdlib.component;
var setTimeout = stdlib.setTimeout;
var setInterval = stdlib.setInterval;
var runAsync = stdlib.runAsync;
var runSync = stdlib.runSync;
var createItem = stdlib.createItem;
var cache = stdlib.cache;
var dotenv = stdlib.dotenv;
var http = stdlib.http;
var sql = stdlib.sql;
var mongo = stdlib.mongo;
var redis = stdlib.redis;

exports = stdlib;
