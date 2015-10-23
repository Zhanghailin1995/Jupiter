package org.jupiter.benchmark.tcp;

import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.netty.NettyConnector;
import org.jupiter.transport.netty.JNettyTcpConnector;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 其中一次简单的测试结果(小包数据1000w+次同步调用)
 * ------------------------------------------------------------------
 * 测试机器:
 * server端(一台机器)
 *      cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
 *      cpu cores: 4
 *
 * client端(一台机器)
 *      cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
 *      cpu cores: 4
 *
 * 网络环境: 局域网
 * ------------------------------------------------------------------
 * 测试结果:
 * Request count: 12800000, time: 117 second, qps: 109401
 *
 * jupiter
 * org.jupiter.benchmark.tcp
 *
 * @author jiachun.fjc
 */
public class BenchmarkClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BenchmarkClient.class);

    public static void main(String[] args) {
        int processors = Runtime.getRuntime().availableProcessors();
        SystemPropertyUtil
                .setProperty("jupiter.processor.executor.core.num.workers", String.valueOf(processors));

        NettyConnector connector = new JNettyTcpConnector();
        UnresolvedAddress[] addresses = new UnresolvedAddress[processors];
        for (int i = 0; i < processors; i++) {
            addresses[i] = new UnresolvedAddress("127.0.0.1", 18090);
            connector.connect(addresses[i]);
        }

        final Service service = ProxyFactory
                .create()
                .connector(connector)
                .addProviderAddress(addresses)
                .interfaceClass(Service.class)
                .newProxyInstance();

        for (int i = 0; i < 10000; i++) {
            try {
                service.hello("jupiter");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final int t = 50000;
        long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(processors << 6);
        final AtomicLong count = new AtomicLong();
        for (int i = 0; i < (processors << 6); i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    for (int i = 0; i < t; i++) {
                        try {
                            long s = SystemClock.millisClock().now();

                            String result = service.hello("jupiter");

                            if (logger.isInfoEnabled()) {
                                logger.info(result + " time cost=" + (SystemClock.millisClock().now() - s));
                            }
                            if (count.getAndIncrement() % 5000 == 0) {
                                logger.warn("count=" + count.get());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    latch.countDown();
                }
            }).start();
        }
        try {
            latch.await();
            logger.warn("count=" + count.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long second = (System.currentTimeMillis() - start) / 1000;
        System.err.println("Request count: " + count.get() + ", time: " + second + " second, qps: " + count.get() / second);
    }
}
