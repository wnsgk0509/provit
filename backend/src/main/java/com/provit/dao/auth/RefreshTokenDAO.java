package com.provit.dao.auth;

import com.provit.dto.auth.RefreshTokenDTO;

public interface RefreshTokenDAO {

    int insert(RefreshTokenDTO refreshToken);

    RefreshTokenDTO selectByTokenId(String tokenId);

    int deleteByTokenId(String tokenId);

    int deleteExpired();
}
