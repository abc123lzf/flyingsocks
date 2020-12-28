package com.lzf.flyingsocks.client.gui.swt;

import org.apache.commons.validator.routines.DomainValidator;

public class Demo2 {

    public static void main(String[] args) throws Exception {
        System.out.println(DomainValidator.getInstance().isValid("baidu.com:80"));
    }

}
