package org.tantama.anchoco.springcrib.ldap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;

/**
 * 独自機構のためのLDAP認証処理<br>
 * 指定のLDAPに対して検索を行い<br>
 * その検索結果の1件目のdnに対してパスワード認証を行う
 *
 * @see AbstractLdapAuthenticationProvider
 * @see DefaultSpringSecurityContextSource
 * @see BindAuthenticator
 */
@Slf4j
public class CustomLdapAuthenticationProvider extends AbstractLdapAuthenticationProvider {

    // notice.

    // LDAP(複数可)に対して、指定の検索条件(複数可)で検索を行い
    // 検索結果のdnに対してパスワード認証を行うという仕様に基づいた機能

    // 認証目的以外の取得は行わないため、loadUserAuthoritiesは無効

    // xmlとして、ldap.xml参照

    /** idをkeyとした{@link LdapConfig} */
    private final Map<String, LdapConfig> configMap;

    /** 認証用LDAPのurl */
    private final String authProviderUrl;

    /** 認証用LDAPのbaseDn */
    private final String authProviderBindDn;

    /**
     * コンストラクタ
     *
     * @param configList         検索用LDAPの設定リスト
     * @param authProviderUrl    認証用LDAPのurl
     * @param authProviderBindDn 認証用LDAPのbaseDn
     */
    @Autowired
    public CustomLdapAuthenticationProvider(List<LdapConfig> configList, String authProviderUrl, String authProviderBindDn) {

        // 登録(設定)順にアクセスしていくのでLinkedHashMapとして作成
        Map<String, LdapConfig> tempMap = new LinkedHashMap<>();

        // 設定リストの分接続情報を作成
        for (LdapConfig config : configList) {
            tempMap.put(config.getId(), config);
        }

        this.authProviderUrl = authProviderUrl;
        this.authProviderBindDn = authProviderBindDn;
        this.configMap = Collections.unmodifiableMap(tempMap);
    }

    @Override
    protected DirContextOperations doAuthentication(UsernamePasswordAuthenticationToken authentication) {

        String name = authentication.getName();

        log.debug("name : {}", name);

        String userDn = getDn(name);

        log.debug("dn : {}", userDn);

        // 認証失敗(dn取得できず)はbad credential
        if (StringUtils.isEmpty(userDn)) {
            throw new BadCredentialsException("dnが存在しない。name : " + name);
        }

        // この辺でLdapAuthenticatorを作る
        // 認証都合上dnが動的なため、認証の度にインスタンスを作成する
        LdapAuthenticator authenticator = createAuthenticator(userDn);

        // ldap認証の実施
        return authenticator.authenticate(authentication);
    }

    /**
     * LDAPのAuthenticatorを作成する
     *
     * @param dn 認証を行うdn
     * @return {@link LdapAuthenticator}
     */
    private LdapAuthenticator createAuthenticator(String dn) {

        LdapContextSource contextSource = new DefaultSpringSecurityContextSource(this.authProviderUrl);
        contextSource.setUserDn(this.authProviderBindDn);
        contextSource.setUserDn(dn);
        contextSource.afterPropertiesSet();

        // ここで引数無しのフォーマットとしてdnを設定しているため
        // 実際の認証処理のBindAuthenticator#authenticate()ではユーザー名は利用されない
        // tips. 引数ありの場合、「cn={0}」のような形
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[] { dn });

        return authenticator;
    }

    /**
     * LDAPに対して、存在するかの検索をかけ<br>
     * 検索結果が存在する場合、dnを返却<br>
     * 検索結果がない場合{@code null}を返却する
     *
     * @param name 検索されるID
     * @return 検索結果のdn(複数あっても検索用LDAPの設定順の初めの1件)
     */
    private String getDn(String name) {

        for (Map.Entry<String, LdapConfig> entry : this.configMap.entrySet()) {

            String ldapId = entry.getKey();

            log.debug("search for {}", entry.getKey());

            LdapConfig config = entry.getValue();
            LdapTemplate template = config.getTemplate();

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter(config.getFilter(), name));

            // ldapに検索
            List<String> dnList = template.search(
                    config.getBaseDn(),
                    filter.encode(),
                    new AbstractContextMapper<String>() {
                        // 取得結果のdn(Distinguish Name)を取得する
                        // tips. 属性取得を行いたい場合はmapFromAttributesなどで取得を行う
                        public String doMapFromContext(DirContextOperations ctx) {
                            // dnすべてを文字列として取得するため、Nameをstring変換
                            String dn = String.valueOf(ctx.getDn());
                            log.debug("search successful. id : {},  dn : {}", ldapId, dn);
                            return dn;
                        }
                    });

            log.debug("search result　id {} count : {}", ldapId, dnList.size());

            if (dnList.isEmpty()) {
                // 見つからない場合は次のLDAP
                continue;
            }

            // 取得結果の1件目を設定(複数あっても初めの1件のみ対象とする)
            return dnList.get(0);
        }

        // 検索結果はなし
        log.warn("ldap search missing.");
        return null;
    }

    /**
     * 認証以外でのLDAP利用させないため<br>
     * 明示的に{@link AuthorityUtils.NO_AUTHORITIES}を返却する
     *
     * @see NullLdapAuthoritiesPopulator
     */
    @Override
    protected Collection<? extends GrantedAuthority> loadUserAuthorities(DirContextOperations userData, String username, String password) {
        // NullLdapAuthoritiesPopulatorと同じ処理
        return AuthorityUtils.NO_AUTHORITIES;
    }

}
