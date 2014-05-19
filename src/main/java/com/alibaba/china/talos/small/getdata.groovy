package com.alibaba.china.talos.small;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
/**
 * get data's sample
 */
public class DataGetter {

    private static final SmallLogger data = new SmallLogger("/data/my.data");

    @SuppressWarnings("unchecked")
    public void execute() {
        JdbcTemplate template = Daoer.getJdbcTemplate();
        List<String> atomAuthIds = template.queryForList("select id from atom_auth_instance where package_code in ('BRAND','MULTICERTS','BANKREMIT') and status='process' and is_deleted='n'",
                                                         String.class);
        if (atomAuthIds != null && !atomAuthIds.isEmpty()) {
            for (String atomAuthId : atomAuthIds) {
                data.info(atomAuthId);
            }
        }
    }
}