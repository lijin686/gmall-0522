package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;


@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String publicPath;
    private String privatePath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String nickName;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init(){
        try {
            File pubFile = new File(publicPath);
            File priFile = new File(privatePath);
            if(!pubFile.exists() || !priFile.exists()){
                RsaUtils.generateKey(publicPath,privatePath,secret);
            }
            this.publicKey = RsaUtils.getPublicKey(publicPath);
            this.privateKey = RsaUtils.getPrivateKey(privatePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}










