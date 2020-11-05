package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "F:\\Java\\gmall-0522\\project-0522\\rsa\\rsa.pub";
    private static final String priKeyPath = "F:\\Java\\gmall-0522\\project-0522\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "#$#332#$sfsdSDSD%");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDQ0NTExOTB9.Ha9iUOEHRZd9dNC2cPDlcpsEuaW3j7XDnzT03zBoS9Jk2H4g3uKxGGGUd2Ewsbuoa9sycPSnVSiV9jogkrOLOZ-6RF5dodZBZeJ0hbTA0K5mITXrwtxxL_vy1rMKwVv6qachr6nly9JCk5ISjDIUQPlS-n1oqBDYRdccRXfHx3-ZQvC1bal68peIm_EQkcwG0UC9oLb6-8IxOkNqyvu2nEuhM2MtPJ8dAKFCTNWeYZPY1mVlgd2r4fli8yt_MfQMvyYUFkRl-iZNg5ScJazWVIMH1ZfGVKU1UqY-txTmy21hiVzjVRRm5gHqfcrMQP8pi2_G-ASI9De1nrxmlfn4cw";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
