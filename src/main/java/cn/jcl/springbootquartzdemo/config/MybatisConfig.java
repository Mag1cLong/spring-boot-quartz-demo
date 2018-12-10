package cn.jcl.springbootquartzdemo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jcl on 2018/12/7
 */
@Configuration
@MapperScan("cn.jcl.springbootquartzdemo.mapper")
public class MybatisConfig {
}
