package com.xoassets.common.security;

/**
 * 登录用户上下文载体，只保存鉴权和数据隔离所需的最小字段。
 */
public record LoginUser(Long userId, String username) {
}
