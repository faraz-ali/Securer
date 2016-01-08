package com.securer.services;

import com.securer.model.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by faraz on 12/26/15.
 */

@Repository
public interface AccessTokenRepository extends MongoRepository<AccessToken, String> {
    //AccessToken findByAccess_Token(String access_token);
}
