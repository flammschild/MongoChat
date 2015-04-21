

Preparation
-----------

Setting up a mongodb replication cluster for testing purposes on a Windows machine (for other operating systems change the dbpath accordingly) and create the MessageCollection. 

   mkdir \data\rs0-0
   mkdir \data\rs0-1
   mkdir \data\rs0-2
   mkdir \data\rs0-3
   
   mongod --port 27017 --dbpath /data/rs0-0 --replSet rs0 --smallfiles --oplogSize 128
   mongod --port 27018 --dbpath /data/rs0-1 --replSet rs0 --smallfiles --oplogSize 128
   mongod --port 27019 --dbpath /data/rs0-2 --replSet rs0 --smallfiles --oplogSize 128
   
   mongo --port 27017
   rsconf = { _id: "rs0", members: [ { _id: 0, host: "192.168.200.131:27017" } ] }
   rs.initiate(rsconf)
   rs.add("192.168.200.131:27018")
   rs.add("192.168.200.131:27019")
   db.createCollection("messages", {capped:true, autoIndexId:false, size:100000, max:20})
