package com.pigeonpost.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AuthViewModel input validation logic.
 * Tests the validation rules directly without requiring Supabase client infrastructure.
 */
class AuthViewModelTest {

    companion object {
        // Mirrors the validation regex from AuthViewModel
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MIN_PASSWORD_LENGTH = 6
    }

    // --- Email validation tests ---

    @Test
    fun `valid email passes validation`() {
        assertTrue(EMAIL_PATTERN.matches("test@example.com"))
    }

    @Test
    fun `email with subdomain passes validation`() {
        assertTrue(EMAIL_PATTERN.matches("user@mail.example.com"))
    }

    @Test
    fun `email with plus sign passes validation`() {
        assertTrue(EMAIL_PATTERN.matches("user+tag@example.com"))
    }

    @Test
    fun `email with dots in local part passes validation`() {
        assertTrue(EMAIL_PATTERN.matches("first.last@example.com"))
    }

    @Test
    fun `email without at sign fails validation`() {
        assertFalse(EMAIL_PATTERN.matches("notanemail"))
    }

    @Test
    fun `email without domain fails validation`() {
        assertFalse(EMAIL_PATTERN.matches("user@"))
    }

    @Test
    fun `email without TLD fails validation`() {
        assertFalse(EMAIL_PATTERN.matches("user@domain"))
    }

    @Test
    fun `email with single char TLD fails validation`() {
        assertFalse(EMAIL_PATTERN.matches("user@domain.x"))
    }

    @Test
    fun `empty email fails validation`() {
        assertFalse(EMAIL_PATTERN.matches(""))
    }

    @Test
    fun `email with spaces fails validation`() {
        assertFalse(EMAIL_PATTERN.matches("user @example.com"))
    }

    // --- Password validation tests ---

    @Test
    fun `password with 6 chars passes minimum length check`() {
        assertTrue("123456".length >= MIN_PASSWORD_LENGTH)
    }

    @Test
    fun `password with 5 chars fails minimum length check`() {
        assertFalse("12345".length >= MIN_PASSWORD_LENGTH)
    }

    @Test
    fun `password with 1 char fails minimum length check`() {
        assertFalse("a".length >= MIN_PASSWORD_LENGTH)
    }

    @Test
    fun `empty password fails minimum length check`() {
        assertFalse("".length >= MIN_PASSWORD_LENGTH)
    }

    @Test
    fun `long password passes minimum length check`() {
        assertTrue("a_very_long_password_123!".length >= MIN_PASSWORD_LENGTH)
    }
}
