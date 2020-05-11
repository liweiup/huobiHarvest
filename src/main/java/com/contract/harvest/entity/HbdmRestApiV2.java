package com.contract.harvest.entity;

import com.huobi.common.api.HbdmRestApiV1;
import com.huobi.common.util.HbdmHttpClient;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HbdmRestApiV2 extends HbdmRestApiV1 {
    public HbdmRestApiV2(String url_prex, String api_key, String secret_key) {
        super(url_prex, api_key, secret_key);
    }

    public HbdmRestApiV2(String url_prex) {
        super(url_prex);
    }
    public String futureContractOpenorders(String symbol) throws HttpException, IOException {
//        System.out.println(super.secret_key);
        return null;
//        Map<String, String> params = new HashMap();
//        if (!StringUtils.isEmpty(symbol)) {
//            params.put("symbol", symbol);
//        }
//
//        if (!StringUtils.isEmpty(pageIndex)) {
//            params.put("page_index", pageIndex);
//        }
//
//        if (!StringUtils.isEmpty(pageSize)) {
//            params.put("page_size", pageSize);
//        }
//
//        String res = HbdmHttpClient.getInstance().call(this.api_key, this.secret_key, "POST", this.url_prex + "/api/v1/contract_openorders", params, new HashMap());
//        return res;
    }
}
