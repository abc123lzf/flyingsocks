package com.lzf.flyingsocks.test;

import org.junit.Test;

import java.net.Inet6Address;
import java.net.UnknownHostException;

/**
 * @author lizifan lzf@webull.com
 * @create 2019.10.6 3:08
 * @description
 */
public class InetTest {

    @Test
    public void test() throws UnknownHostException {
        //System.setProperty("sun.net.spi.nameservice.nameservers", "114.114.114.114");
        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");

        for (int i = 0; i < 10; i++) {
            long st = System.currentTimeMillis();
            System.out.println(Inet6Address.getByName("ipv6.google.com").getHostAddress());
            long ed = System.currentTimeMillis();
            System.out.println(ed - st);
        }

    }

}
