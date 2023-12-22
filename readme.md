# ProactMP
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)

# To build the module:
1. Download and install ZooKeeper:

https://archive.apache.org/dist/zookeeper/


2. Configure environment variables:
```
export JAVA_HOME=/usr/bin/java

export ZOOKEEPER_HOME=/www/wwwroot/apache-zookeeper-3.5.7-bin // zk

export PATH=$JAVA_HOME/bin:$PATH:%ZOOKEEPER_HOME/bin
```

3. Start ZooKeeper:
```
    sh zkServer.sh start

    sh zkServer.sh status // to check the status
```
(/bin)
```
    ./zkServer.sh start

    ./zkServer.sh status
```

4. Project configuration:

**dubbo-consumer**

```
dubbo:

  application:

    name: dubbo-springboot-demo-consumer

  protocol:

    port: -1

    name: dubbo

    payload: 104857600

    prefer-serialization: hession2

  registry:

    address: zookeeper://xxxxx:2181    # change xxxxx to your ip

server:

  port: 2395
```

**dubbo-provider**

```
dubbo:

  application:

    name: dubbo-springboot-demo-provider

  registry:

    address: zookeeper://xxxxx:2181    # change xxxxx to your ip

  protocol:

    port: -1

    name: dubbo

    payload: 104857600

    prefer-serialization: hession2
```


5. Start the project:

Package "consumer" and "provider" through the mvn package method of IDEA (IntelliJ IDEA).

Due to "interfaces" being a public module, local dependencies are required. If the symbol cannot be found, please 'mvn install' interfaces first.

Interface: 2395


6. Set OWDbytes:

Request method: POST

Request path: setRQFS

Parameter: requestFileSize    // set requestFileSize to OWDbytes

// ProactMP enters pre-grant phase and blindly sends OWDbytes of data in the first RTT

// "OWDbytes" can be changed according to downlink bandwidth * one-way delay

For example: http://192.168.1.10:2395/setRQFS?requestFileSize=5


7. Set downlink bandwidth:

Parameter: rate_credits    // set rate_credits to downlink bandwidth (unit: mb/ms)

Parameter: w    // set w to rate_credits (downlink bandwidth)

Parameter: c    // set c to OWDbytes


8. Set timeout for loss recovery:

consumer --> FileController --> timeout (unit: ms)


# To request and download files:
Request method: POST

Parameter: from    // the address of the requested file

Parameter: to    // download to the specified path

// {requested url} + {filename}

For example: http://192.168.1.10:2395/?from=/192.168.17.1/www/test.txt&to=/www/project

