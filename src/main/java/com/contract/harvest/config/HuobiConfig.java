package com.contract.harvest.config;

import com.contract.harvest.Aspects.HuobiServiceAspects;
import com.huobi.common.api.HbdmRestApiV1;
import com.huobi.common.api.IHbdmRestApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;

@EnableAspectJAutoProxy
@Configuration
public class HuobiConfig {

    @Value("${huobi.api_key}")
    private String huobi_api_key;

    @Value("${huobi.secret_key}")
    private String huobi_secret_key;

    @Value("${huobi.huobi_api_url}")
    private String huobi_api_url;

    @Bean("futureGetV1")
    public IHbdmRestApi futureGetV1() {
        return new HbdmRestApiV1(huobi_api_url);
    }

	@Bean("futurePostV1")
	public IHbdmRestApi futurePostV1() {
		return new HbdmRestApiV1(huobi_api_url, huobi_api_key, huobi_secret_key);
	}

    @Bean
    public HuobiServiceAspects huobiServiceAspects(){
        return new HuobiServiceAspects();
    }
}
