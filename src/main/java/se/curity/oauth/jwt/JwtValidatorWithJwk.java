/*
 * Copyright (C) 2016 Curity AB.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.curity.oauth.jwt;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * A validator class that does not depend on any external libraries
 */
public final class JwtValidatorWithJwk extends AbstractJwtValidator
{

    private static final Logger _logger = Logger.getLogger(JwtValidatorWithJwk.class.getName());

    private final Gson _gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private final JwkManager _jwkManager;

    public JwtValidatorWithJwk(URI webKeysURI, long minKidReloadTime, HttpClient httpClient)
    {
        _jwkManager = new JwkManager(webKeysURI, minKidReloadTime, httpClient);
    }

    /**
     * JWT: abads.gfdr.htefgy
     *
     * @param jwt the original base64 encoded JWT
     * @return A map with the content of the Jwt body if the JWT is valid, otherwise null
     * @throws IllegalArgumentException if alg or kid are not present in the JWT header.
     * @throws RuntimeException         if some environment issue makes it impossible to validate a signature
     */
    public Map<String,Object> validate(String jwt) throws JwtValidationException
    {
        String[] jwtParts = jwt.split("\\.");

        if (jwtParts.length != 3)
        {
            throw new IllegalArgumentException("Incorrect JWT input");
        }

        String header = jwtParts[0];
        String body = jwtParts[1];
        byte[] headerAndPayload = convertToBytes(header + "." + jwtParts[1]);

        Base64 base64 = new Base64(true);

        Map<?, ?> headerMap = _gson.fromJson(new String(base64.decode(header), Charsets.UTF_8), Map.class);

        Object alg = headerMap.get("alg");
        Object kid = headerMap.get("kid");

        Preconditions.checkNotNull(alg, "Alg is not present in JWT");
        Preconditions.checkNotNull(kid, "Key ID is not present in JWT");

        if (canRecognizeAlg(alg.toString()))
        {
            try
            {
                Optional<JsonWebKey> maybeWebKey = getJsonWebKeyFor(kid.toString());

                if (maybeWebKey.isPresent())
                {
                    byte[] signatureData = base64.decode(jwtParts[2]);

                    if(validateSignature(headerAndPayload,
                            signatureData,
                            getKeyFromModAndExp(maybeWebKey.get().getModulus(), maybeWebKey.get().getExponent())))
                    {
                        Map<?, ?> map = _gson.fromJson(new String(base64.decode(body), Charsets.UTF_8), Map.class);
                        Map<String, Object> result = new LinkedHashMap<>(map.size());

                        for (Map.Entry entry : map.entrySet())
                        {
                            result.put(entry.getKey().toString(), entry.getValue());
                        }

                        return result;
                    }
                }
            }
            catch(Exception e)
            {
                throw new JwtValidationException("Unable to validate Jwt ", e);
            }
        }
        else
        {
            _logger.info(() -> String.format("Requested JsonWebKey using unrecognizable alg: %s", headerMap.get("alg")));
        }

        return Collections.emptyMap();
    }

    private Optional<JsonWebKey> getJsonWebKeyFor(String kid)
    {
        try
        {
            return Optional.ofNullable(_jwkManager.getJsonWebKeyForKeyId(kid));
        }
        catch (JsonWebKeyNotFoundException e)
        {
            // this is not a very exceptional occurrence, so let's not log a stack-trace
            _logger.info(() -> String.format("Could not find requested JsonWebKey: %s", e));

            return Optional.empty();
        }
    }

    private boolean canRecognizeAlg(String alg)
    {
        return alg.equals("RS256");
    }

    @Override
    public void close() throws IOException
    {
        _jwkManager.close();
    }
}
