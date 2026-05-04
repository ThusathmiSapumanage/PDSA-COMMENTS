package com.pdsa.games.common.algorithmExecute;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmExecuteId implements Serializable {
    private Integer sessionId;
    private Integer algorithmId;
}
