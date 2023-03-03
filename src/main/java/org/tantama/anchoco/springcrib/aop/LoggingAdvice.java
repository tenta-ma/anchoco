package org.tantama.anchoco.springcrib.aop;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * aspect logger
 */
@Slf4j
@Aspect
@Component
public class LoggingAdvice {

    /**
     * コントローラーの処理前ログ<br>
     * サーバーログテーブルにアクセス情報を登録する
     *
     * @param joinPoint joinPoint
     */
    @Before("execution(* org.tantama.anchoco.springcrib.controller.*.*(..))")
    public void beforeContoller(JoinPoint joinPoint) {
        log.debug("start contoller method : {}", joinPoint.toShortString());
    }

    /**
     * コントローラーの正常終了後ログ
     *
     * @param joinPoint joinPoint
     */
    @AfterReturning(pointcut = "execution(* org.tantama.anchoco.springcrib.controller.*.*(..))")
    public void successContoller(JoinPoint joinPoint) {
        log.debug("success contoller method : {}", joinPoint.toShortString());
    }

    /**
     * コントローラーのエラー発生終了後ログ
     *
     * @param joinPoint
     * @param e         発生エラー
     */
    @AfterThrowing(value = "execution(* org.tantama.anchoco.springcrib.controller.*.*(..))", throwing = "e")
    public void abortContoller(JoinPoint joinPoint, Throwable e) {
        log.warn("abort contoller method : {}", joinPoint.toShortString());
    }

    /**
     * サービスの処理前ログ<br>
     * 実装クラス(*Impl)のみ対象
     *
     * @param joinPoint joinPoint
     */
    @Before("execution(* org.tantama.anchoco.springcrib.service.*Impl.*(..))")
    public void beforeService(JoinPoint joinPoint) {
        log.debug("start service method : {}", joinPoint.toShortString());
    }

    /**
     * サービスの正常終了後ログ
     *
     * @param joinPoint joinPoint
     */
    @AfterReturning(pointcut = "execution(* org.tantama.anchoco.springcrib.service.*Impl.*(..))")
    public void successService(JoinPoint joinPoint) {
        log.debug("success service method : {}", joinPoint.toShortString());
    }

    /**
     * サービスのエラー発生終了後ログ
     *
     * @param joinPoint
     * @param e         発生エラー
     */
    @AfterThrowing(value = "execution(* org.tantama.anchoco.springcrib.service.*Impl.*(..))", throwing = "e")
    public void abortService(JoinPoint joinPoint, Throwable e) {
        log.warn("abort service method : {}", joinPoint.toShortString());
    }

}
