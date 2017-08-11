package com.zzg.mybatis.generator.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Owen on 6/18/16.
 */
public class StringUtilTest {

    @Test
    public void testDbStringToCamelStyle() {
        String result = StringUtils.dbStringToCamelStyle("person_address");
        Assert.assertEquals("PersonAddress", result);
    }

    @Test
    public void testDbStringToCamelStyle_case2() {
        String result = StringUtils.dbStringToCamelStyle("person_address_name");
        Assert.assertEquals("PersonAddressName", result);
    }

    @Test
    public void testDbStringToCamelStyle_case3() {
        String result = StringUtils.dbStringToCamelStyle("person_DB_name");
        Assert.assertEquals("PersonDBName", result);
    }

    @Test
    public void testDbStringToCamelStyle_case4() {
        String result = StringUtils.dbStringToCamelStyle("person_jobs_");
        Assert.assertEquals("PersonJobs", result);
    }

    @Test
    public void testDbStringToCamelStyle_case5() {
        String result = StringUtils.dbStringToCamelStyle("a");
        Assert.assertEquals("A", result);
    }

}
