package com.lzf.flyingsocks.test;

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.util.BaseUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.*;

public class GFWListAutoProxyTest {

    private final String[] tests = new String[] {
            "www.google.com", "www.youtube.com", "facebook.com", "www.baidu.com", "google.co.jp", "google.com"
    };

    private final NavigableSet<String> set = new TreeSet<>();
    private final Set<String> set0 = new HashSet<>(8192);

    @Before
    public void load() throws Exception {
        try {
            Class.forName("com.lzf.flyingsocks.url.ClasspathURLHandlerFactory");
        } catch (ClassNotFoundException e) {
            throw new ComponentException(e);
        }

        URL url = new URL("classpath://pac.txt");
        try (Scanner sc = new Scanner(url.openStream())) {
            while (sc.hasNext()) {
                String str = sc.next();
                set.add(new StringBuilder(str).reverse().toString());
                set0.add(str);
            }
        }
    }

    @Test
    public void test() {
        long st = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            for (String host : tests) {
                //String rh = host;
                host = new StringBuilder(host).reverse().toString();
                String low = set.floor(host);
                if (low == null || !host.startsWith(low)) {
                    //System.out.println(rh + " " + false);
                } else {
                    //System.out.println(rh + " " + true);
                }
            }
        }
        long ed = System.currentTimeMillis();
        System.out.println(ed - st + " ms");
    }

    @Test
    public void test0() {
        long st = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            for (String host : tests) {
                if (BaseUtils.isIPAddress(host)) {
                    //System.out.println(host + " " + true);
                    continue;
                }
                String[] hs = BaseUtils.splitPreserveAllTokens(host, '.');
                int len = hs.length;
                if (len <= 1) {
                    System.out.println(host + " " + false);
                    continue;
                }
                String realHost = hs[len - 2] + '.' + hs[len - 1];
                if (set0.contains(realHost)) {
                    //System.out.println(host + " " + true);

                } else if (realHost.length() < 7 && len >= 3) {  //解决部分域名为xxx.co.jp情况
                    //System.out.println(host + " " + set0.contains(hs[len - 3] + '.' + realHost));
                } else {
                    //System.out.println(host + " " + false);
                }
            }
        }
        long ed = System.currentTimeMillis();
        System.out.println(ed - st + " ms");
    }

}
