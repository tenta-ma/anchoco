package org.tantama.anchoco.springcrib.ldap;

import lombok.Getter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * (ユーザー検索用)LDAP設定情報
 */
@Getter
public class LdapConfig {

    /** LDAP識別子 */
    private final String id;

    /** ldap(s):から始まる url */
    private final String url;

    /** 検索用ユーザー(admin)dn */
    private final String userDn;

    /** adminユーザーパスワード */
    private final String password;

    /** ユーザー検索用のbaseDn */
    private final String baseDn;

    /** ユーザー検索用のfilter(format) */
    private final String filter;

    /** {@link LdapTemplate} */
    private final LdapTemplate template;

    /**
     * コンストラクタ
     *
     * @param id       LDAP識別子
     * @param url      ldap url
     * @param userDn   検索用ユーザー(admin)dn
     * @param password adminユーザーパスワード
     * @param baseDn   ユーザー検索用のbaseDn
     * @param filter   ユーザー検索用のfilter(format)
     */
    public LdapConfig(String id, String url, String userDn, String password, String baseDn, String filter) {
        this.id = id;
        this.url = url;
        this.userDn = userDn;
        this.password = password;
        this.baseDn = baseDn;
        this.filter = filter;

        // LdapTemplate setting.
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(this.url);
        contextSource.setUserDn(this.userDn);
        contextSource.setPassword(this.password);
        contextSource.afterPropertiesSet();
        template = new LdapTemplate(contextSource);
    }

}
