/*
 * Copyright (C) 2017 Curity AB.
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

import se.curity.oauth.JsonUtils;
import se.curity.oauth.TokenData;
import se.curity.oauth.opaque.Expirable;

import javax.json.JsonObject;
import java.time.Instant;

public class JwtData extends TokenData implements Expirable
{
    private final JsonObject _jsonObject;

    public JwtData(JsonObject jsonObject)
    {
        _jsonObject = jsonObject;
    }

    public JsonObject getJsonObject()
    {
        return _jsonObject;
    }

    @Override
    public String getSubject()
    {
        return JsonUtils.getString(_jsonObject, "sub");
    }

    @Override
    public String getScope()
    {
        return JsonUtils.getString(_jsonObject, "scope");
    }

    @Override
    public Instant getExpiresAt()
    {
        return Instant.ofEpochSecond(JsonUtils.getLong(_jsonObject, "exp"));
    }
}
