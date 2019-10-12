package com.lzf.flyingsocks.test;

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.util.BaseUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

public class WhiteListAutoProxyTest {

    private final NavigableMap<Integer, Integer> whiteListMap = new TreeMap<>();
    private final Map<Integer, Integer> whiteListMap0 = new HashMap<>(8192);

    private final String[] tests = new String[] {
            "39.108.210.75", "45.32.132.93", "45.31.1.5", "14.215.177.39", "8.8.8.8", "114.114.114.114"
    };

    @Before
    public void load() throws Exception {
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }
        Pattern tar = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,2}");
        try(Scanner sc = new Scanner(new URL("classpath://cnipv4.txt").openStream())) {
            while (sc.hasNext(tar)) {
                String str = sc.next(tar);
                int idx = str.lastIndexOf('/');
                int ip = BaseUtils.parseIPv4StringToInteger(str.substring(0, idx));
                int mask = 0xFFFFFFFF << (32 - Integer.parseInt(str.substring(idx + 1)));
                whiteListMap.put(ip, mask);
                whiteListMap0.put(ip, mask);
            }
        }
    }

    @Test
    public void test() {
        long st = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            for (String ip : tests) {
                check(ip);
                //System.out.println(ip + " " + check(ip));
            }
        }
        long ed = System.currentTimeMillis();
        System.out.println(ed - st + " ms");
    }

    @Test
    public void test0() {
        long st = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            for (String ip : tests) {
                check0(ip);
                //System.out.println(ip + " " + check0(ip));
            }
        }
        long ed = System.currentTimeMillis();
        System.out.println(ed - st + " ms");
    }

    private boolean check(String host) {
        int ip;
        if(BaseUtils.isHostName(host)) {
            try {
                byte[] b = InetAddress.getByName(host).getAddress();
                if(b.length > 4)  //IPV6暂时全局代理
                    return true;
                ip = BaseUtils.parseByteArrayToIPv4Integer(b);
            } catch (UnknownHostException e) {
                return false;
            }

        } else if(BaseUtils.isIPv4Address(host)) {
            ip = BaseUtils.parseIPv4StringToInteger(host);
        } else {
            return true; //暂时IPV6统一代理
        }

        int net = ip & 0xFFFFFF00;
        Integer mask;
        for (int i = 9; i <= 22; i++) {
            net &= (-(1 << i));
            if((mask = whiteListMap.get(net)) != null) {  //如果是中国IP
                if(BaseUtils.isIPv4AddressInRange(ip, net, mask))
                    return false;
            }
        }

        return true;
    }

    private boolean check0(String host) {
        int ip;
        if(BaseUtils.isHostName(host)) {
            try {
                byte[] b = InetAddress.getByName(host).getAddress();
                if(b.length > 4)  //IPV6暂时全局代理
                    return true;
                ip = BaseUtils.parseByteArrayToIPv4Integer(b);
            } catch (UnknownHostException e) {
                return false;
            }

        } else if(BaseUtils.isIPv4Address(host)) {
            ip = BaseUtils.parseIPv4StringToInteger(host);
        } else {
            return true; //暂时IPV6统一代理
        }

        Map.Entry<Integer, Integer> entry = whiteListMap.floorEntry(ip);
        int mask = entry.getValue();
        int net = entry.getKey();

        return !BaseUtils.isIPv4AddressInRange(ip, net, mask);
    }

}
