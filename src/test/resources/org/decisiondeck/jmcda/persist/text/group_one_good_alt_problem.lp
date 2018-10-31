\Problem name: group_one_good_alt

Maximize
 obj: s
Subject To
 yj0a0h0#FLOOR:       yj0a0h0 + 0.5 xj0h0 >= 1
 yj1a0h0#FLOOR:       yj1a0h0 + 0.5 xj1h0 >= 0.5
 yj0a0h0#CEILING:     yj0a0h0 + 0.5 xj0h0 <= 1
 yj1a0h0#CEILING:     yj1a0h0 + 0.5 xj1h0 <= 0.5
 zl0j0a0h0#CEILING_C: - yj0a0h0 + zl0j0a0h0 <= 0
 zl0j0a0h0#CEILING_W: zl0j0a0h0 - wj0l0 <= 0
 zl0j0a0h0#BOTTOM:    - yj0a0h0 + zl0j0a0h0 - wj0l0 >= -1
 zl0j1a0h0#CEILING_C: - yj1a0h0 + zl0j1a0h0 <= 0
 zl0j1a0h0#CEILING_W: zl0j1a0h0 - wj1l0 <= 0
 zl0j1a0h0#BOTTOM:    - yj1a0h0 + zl0j1a0h0 - wj1l0 >= -1
 supportl0a0h0#FLOOR: zl0j0a0h0 + zl0j1a0h0 - λl0 - s >= 0
 weightsl0#SUM:       wj0l0 + wj1l0  = 1
 xj0h0#MIN:           xj0h0 >= 0
 xj0h0#MAX:           xj0h0 <= 2
 xj1h0#MIN:           xj1h0 >= -1
 xj1h0#MAX:           xj1h0 <= 1
Bounds
 0 <= yj0a0h0 <= 1
 0 <= yj1a0h0 <= 1
      xj0h0 Free
      xj1h0 Free
 0 <= zl0j0a0h0 <= 1
 0 <= zl0j1a0h0 <= 1
 0.5 <= λl0 <= 1
 0 <= wj0l0 <= 1
 0 <= wj1l0 <= 1
Binaries
 yj0a0h0  yj1a0h0 
End
