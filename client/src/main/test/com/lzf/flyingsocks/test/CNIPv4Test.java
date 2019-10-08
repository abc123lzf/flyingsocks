package com.lzf.flyingsocks.test;

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.util.BaseUtils;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * @author lizifan lzf@webull.com
 * @create 2019.10.4 20:44
 * @description
 */
public class CNIPv4Test {

    private Pattern IP_PATTERN;

    private final Map<Integer, Integer> map = new HashMap<>();

    @Before
    public void before() {
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }

        IP_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,2}");
    }

    @Test
    public void simple() throws Exception {
        loadFileTest();

        byte[] b = InetAddress.getByName("ss0.bdstatic.com").getAddress();
        int ip = BaseUtils.parseByteArrayToIPv4Integer(b);

        System.out.println(BaseUtils.parseByteArrayToIPv4Address(b));
        System.out.println(new BigInteger(Long.toString(ip & (long)0x0FFFFFFF)).toString(2));
        int mask = BaseUtils.parseByteArrayToIPv4Integer(new byte[] {(byte) 0xFF, (byte) 0xE0, (byte) 0x00, (byte) 0x00});
        System.out.println(new BigInteger(Long.toString(mask & (long)0x0FFFFFFF)).toString(2) + " " + mask);
        int net = ip & 0xFFFFFF00;
        System.out.println(new BigInteger(Long.toString(net & (long)0x0FFFFFFF)).toString(2));

        boolean flag = false;
        for (int i = 8; i <= 22; i++) {
            net &= (-(1 << i));
            if(BaseUtils.isIPv4AddressInRange(ip, net, mask)) {  //如果是中国IP
                System.out.println(new BigInteger(Long.toString(net & (long)0x0FFFFFFF)).toString(2) + " " + net);
                flag = true;
            }
        }

        System.out.println(flag);
    }

    @Test
    public void simple0() throws Exception {
        byte[] b = InetAddress.getByName("ss0.bdstatic.com").getAddress();
        System.out.println(Arrays.toString(b));
        int ip = BaseUtils.parseByteArrayToIPv4Integer(b);
        System.out.println(ip);
        System.out.println(BaseUtils.parseIntToIPv4Address(ip));
    }

    @Test
    public void loadFileTest() throws Exception {
        Scanner sc = new Scanner(new URL("classpath://cnipv4.txt").openStream());
        while (sc.hasNextLine()) {
            String str = sc.next(IP_PATTERN);
            int idx = str.lastIndexOf('/');
            int ip = BaseUtils.parseIPv4StringToInteger(str.substring(0, idx));
            int mask = 0xFFFFFFFF << (32 - Integer.parseInt(str.substring(idx + 1)));
            map.put(ip, mask);
        }
    }

    @Test
    public void checkout() throws Exception {
        loadFileTest();
        System.out.println(needProxy("sp2.baidu.com"));
    }

    private boolean needProxy(String host) {
        int ip = 0;
        if(BaseUtils.isHostName(host)) {
            try {
                byte[] b = InetAddress.getByName(host).getAddress();
                if(b.length > 4)
                    return true;
                ip = BaseUtils.parseByteArrayToIPv4Integer(b);
            } catch (UnknownHostException e) {
                System.out.println("Unknown host " + host);
            }

        } else if(BaseUtils.isIPv4Address(host)) {
            ip = BaseUtils.parseIPv4StringToInteger(host);
        } else {
            return true; //暂时IPV6统一代理
        }

        //System.out.println(new BigInteger(Long.toString(ip & (long)0x0FFFFFFF)).toString(2));

        int net = ip & 0xFFFFFF00;
        Integer mask;
        for (int i = 9; i <= 22; i++) {
            net &= (-(1 << i));
            //System.out.println(new BigInteger(Long.toString(net & (long)0x0FFFFFFF)).toString(2));
            if((mask = map.get(net)) != null) {  //如果是中国IP
                //System.out.println(net);
                if(BaseUtils.isIPv4AddressInRange(ip, net, mask))
                    return false;
            }
        }

        return true;
    }
}
