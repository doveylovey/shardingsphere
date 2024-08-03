/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sql.parser.statement.core.segment.dcl;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.sql.parser.statement.core.enums.ACLAttributeType;
import org.apache.shardingsphere.sql.parser.statement.core.segment.SQLSegment;

/**
 * Password or lock option segment.
 */
@Getter
@Setter
public final class PasswordOrLockOptionSegment implements SQLSegment {
    
    private int startIndex;
    
    private int stopIndex;
    
    private boolean updatePasswordExpiredFields;
    
    private boolean updatePasswordExpiredColumn;
    
    private boolean useDefaultPasswordLifeTime;
    
    private int expireAfterDays;
    
    private boolean updateAccountLockedColumn;
    
    private boolean accountLocked;
    
    private int passwordHistoryLength;
    
    private boolean useDefaultPasswordHistory;
    
    private boolean updatePasswordHistory;
    
    private int passwordReuseInterval;
    
    private boolean useDefaultPasswordReuseInterval;
    
    private boolean updatePasswordReuseInterval;
    
    private int failedLoginAttempts;
    
    private boolean updateFailedLoginAttempts;
    
    private int passwordLockTime;
    
    private boolean updatePasswordLockTime;
    
    private ACLAttributeType updatePasswordRequireCurrent;
}
