package com.qx.orbit.core.usecase.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class LoginStateMachineTest {

    @Test
    fun `ANONYMOUS on StartLogin goes SCANNING`() {
        assertEquals(
            LoginPhase.SCANNING,
            LoginStateMachine.transition(LoginPhase.ANONYMOUS, LoginEvent.StartLogin)
        )
    }

    @Test
    fun `SCANNING on LoginSuccess goes LOGGED_IN`() {
        assertEquals(
            LoginPhase.LOGGED_IN,
            LoginStateMachine.transition(LoginPhase.SCANNING, LoginEvent.LoginSuccess)
        )
    }

    @Test
    fun `SCANNING on LoginFailed returns ANONYMOUS`() {
        assertEquals(
            LoginPhase.ANONYMOUS,
            LoginStateMachine.transition(LoginPhase.SCANNING, LoginEvent.LoginFailed)
        )
    }

    @Test
    fun `SCANNING on SessionExpired goes EXPIRED`() {
        assertEquals(
            LoginPhase.EXPIRED,
            LoginStateMachine.transition(LoginPhase.SCANNING, LoginEvent.SessionExpired)
        )
    }

    @Test
    fun `LOGGED_IN on SessionExpired goes EXPIRED`() {
        assertEquals(
            LoginPhase.EXPIRED,
            LoginStateMachine.transition(LoginPhase.LOGGED_IN, LoginEvent.SessionExpired)
        )
    }

    @Test
    fun `LOGGED_IN on Logout returns ANONYMOUS`() {
        assertEquals(
            LoginPhase.ANONYMOUS,
            LoginStateMachine.transition(LoginPhase.LOGGED_IN, LoginEvent.Logout)
        )
    }

    @Test
    fun `EXPIRED on StartLogin goes SCANNING`() {
        assertEquals(
            LoginPhase.SCANNING,
            LoginStateMachine.transition(LoginPhase.EXPIRED, LoginEvent.StartLogin)
        )
    }

    @Test
    fun `EXPIRED on Logout returns ANONYMOUS`() {
        assertEquals(
            LoginPhase.ANONYMOUS,
            LoginStateMachine.transition(LoginPhase.EXPIRED, LoginEvent.Logout)
        )
    }

    @Test
    fun `ANONYMOUS on Logout is a harmless no-op`() {
        assertEquals(
            LoginPhase.ANONYMOUS,
            LoginStateMachine.transition(LoginPhase.ANONYMOUS, LoginEvent.Logout)
        )
    }

    @Test
    fun `illegal transitions keep state unchanged`() {
        // ANONYMOUS 时发生成功/失效事件：忽略
        assertEquals(LoginPhase.ANONYMOUS, LoginStateMachine.transition(LoginPhase.ANONYMOUS, LoginEvent.LoginSuccess))
        assertEquals(LoginPhase.ANONYMOUS, LoginStateMachine.transition(LoginPhase.ANONYMOUS, LoginEvent.SessionExpired))
        // SCANNING 时重复开始登录：忽略
        assertEquals(LoginPhase.SCANNING, LoginStateMachine.transition(LoginPhase.SCANNING, LoginEvent.StartLogin))
        assertEquals(LoginPhase.SCANNING, LoginStateMachine.transition(LoginPhase.SCANNING, LoginEvent.Logout))
        // LOGGED_IN 时开始登录/登录失败：忽略
        assertEquals(LoginPhase.LOGGED_IN, LoginStateMachine.transition(LoginPhase.LOGGED_IN, LoginEvent.StartLogin))
        assertEquals(LoginPhase.LOGGED_IN, LoginStateMachine.transition(LoginPhase.LOGGED_IN, LoginEvent.LoginFailed))
        // EXPIRED 时登录成功：忽略
        assertEquals(LoginPhase.EXPIRED, LoginStateMachine.transition(LoginPhase.EXPIRED, LoginEvent.LoginSuccess))
        assertEquals(LoginPhase.EXPIRED, LoginStateMachine.transition(LoginPhase.EXPIRED, LoginEvent.SessionExpired))
    }

    @Test
    fun `typical login lifecycle traverses all phases`() {
        var phase = LoginPhase.ANONYMOUS
        phase = LoginStateMachine.transition(phase, LoginEvent.StartLogin)     // SCANNING
        assertEquals(LoginPhase.SCANNING, phase)
        phase = LoginStateMachine.transition(phase, LoginEvent.SessionExpired) // EXPIRED
        assertEquals(LoginPhase.EXPIRED, phase)
        phase = LoginStateMachine.transition(phase, LoginEvent.StartLogin)     // SCANNING
        phase = LoginStateMachine.transition(phase, LoginEvent.LoginSuccess)   // LOGGED_IN
        assertEquals(LoginPhase.LOGGED_IN, phase)
        phase = LoginStateMachine.transition(phase, LoginEvent.Logout)         // ANONYMOUS
        assertEquals(LoginPhase.ANONYMOUS, phase)
    }
}
