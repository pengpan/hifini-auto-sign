package com.github.pengpan;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.SystemPropsUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.cookie.GlobalCookieManager;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.dialect.PropsUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class App {

    public static void main(String[] args) {
        log.info("=====签到开始=====");

        JSONObject loginResp = doLogin(getProperty("USER_NAME"), getProperty("PASSWORD"));
        boolean loginSuccess = "0".equals(loginResp.getStr("code"));
        log.info(loginSuccess ? "登录成功" : ("登录失败: " + loginResp));

        if (loginSuccess) {
            JSONObject signResp = doSign();
            boolean signSuccess = "0".equals(signResp.getStr("code"));
            log.info(signSuccess ? "签到成功" : ("签到失败: " + signResp));
        }

        log.info("=====签到结束=====");
    }

    private static String getProperty(String key) {
        String value = SystemPropsUtil.get(key);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return PropsUtil.get("config").getProperty(key);
    }

    private static JSONObject doLogin(String username, String password) {
        String loginResp = HttpUtil.createPost("https://www.hifini.com/user-login.htm")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("x-requested-with", "XMLHttpRequest")
                .form("email", username)
                .form("password", DigestUtil.md5Hex(password))
                .timeout(1000 * 60)
                .execute()
                .body();
        return Optional.ofNullable(loginResp).map(JSONUtil::parseObj).orElseGet(JSONObject::new);
    }

    private static JSONObject doSign() {
        String signResp = HttpUtil.createPost("https://www.hifini.com/sg_sign.htm")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("x-requested-with", "XMLHttpRequest")
                .form("sign", getSign())
                .cookie(GlobalCookieManager.getCookieManager().getCookieStore().getCookies())
                .timeout(1000 * 60)
                .execute()
                .body();
        return Optional.ofNullable(signResp).map(JSONUtil::parseObj).orElseGet(JSONObject::new);
    }

    private static String getSign() {
        String indexResp = HttpUtil.createGet("https://www.hifini.com")
                .cookie(GlobalCookieManager.getCookieManager().getCookieStore().getCookies())
                .timeout(1000 * 60)
                .execute()
                .body();
        return ReUtil.get("var sign = \"([^\"]+)\";", indexResp, 1);
    }
}
