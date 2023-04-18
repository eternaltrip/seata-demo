package cn.itcast.account.service;


import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;


/**
 * TCC模式，就是try-confirm-cancel模式，就是把要操作的数据进行修改，并把修改的部分用一个数据表保存，模拟出“冻结”操作。然后根据情况决定是否提交、回滚.
 * 注意：空回滚，如何避免业务悬挂
 *
 *
 * 操作步骤如下：
 * 1.try
 *  1.1获取全局事务id
 *  1.2进行数据库扣款操作
 *  1.3把扣款的部分写入到冻结表中
 * 2.confirm
 *  2.1 根据事务id删除冻结的部分
 * 3.cancel
 *  3.1 恢复冻结部分数据到原数据表中
 *  3.2 修改冻结数据的金额为0，修改状态为cancel。为了保证空回滚的操作可行性
 */
@LocalTCC
public interface AccountTCCService {


    @TwoPhaseBusinessAction(name="deduct" , commitMethod = "comfirm" , rollbackMethod = "cancel")
    void deduct(@BusinessActionContextParameter(paramName = "userId") String userId,
                @BusinessActionContextParameter(paramName = "money") int money);

    boolean comfirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
