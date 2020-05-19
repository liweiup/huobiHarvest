package com.contract.harvest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@MapperScan("com.contract.harvest.mapper")
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class HarvestApplication {

	//设置时区 相差8小时
	@PostConstruct
	void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC+8"));
	}

	public static void main(String[] args) {
		SpringApplication.run(HarvestApplication.class, args);
	}

}
