package com.smartfarm.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 — 分页插件 + Mapper扫描
 *
 * @author SmartFarm Team
 */
@Configuration
@MapperScan("com.smartfarm.business.mapper")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器
     * <p>
     * PaginationInnerInterceptor: 分页插件（自动拦截分页查询，生成 COUNT + LIMIT SQL）
     * DbType.MYSQL: 针对MySQL优化分页方言
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大500条，防止恶意全量查询
        paginationInterceptor.setMaxLimit(500L);
        // 溢出总页数后自动回到首页
        paginationInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
