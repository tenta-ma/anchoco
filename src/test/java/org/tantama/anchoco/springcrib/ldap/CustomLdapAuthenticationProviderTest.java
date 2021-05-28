package org.tantama.anchoco.springcrib.ldap;

import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link CustomLdapAuthenticationProvider}のテストクラス
 */
public class CustomLdapAuthenticationProviderTest {

    /** テスト処理対象クラス */
    private CustomLdapAuthenticationProvider target;

    /**
     * 初期処理
     */
    @BeforeEach
    public void init() {
        target = new CustomLdapAuthenticationProvider(List.of(), StringUtils.EMPTY, StringUtils.EMPTY);
    }

    /**
     * {@link CustomLdapAuthenticationProvider#doAuthentication(UsernamePasswordAuthenticationToken)}のテスト
     */
    @Test
    @DisplayName("認証実行")
    @Disabled("LDAPがないと難しいため、実施不能")
    public void testDoAuthentication() {
        fail("テスト未実装");
    }

    /**
     * {@link CustomLdapAuthenticationProvider#loadUserAuthorities(DirContextOperations, String, String)}のテスト
     */
    @Test
    @DisplayName("loadUserAuthoritiesのテスト")
    public void testLoadUserAuthorities() {

        DirContextOperations operations = Mockito.mock(DirContextOperations.class);
        final int strLen = 10;
        final String username = RandomStringUtils.random(strLen);
        final String password = RandomStringUtils.random(strLen);

        assertEquals(AuthorityUtils.NO_AUTHORITIES, target.loadUserAuthorities(operations, username, password));
    }

}
