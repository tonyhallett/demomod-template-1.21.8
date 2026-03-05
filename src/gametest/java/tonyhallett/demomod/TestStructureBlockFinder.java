package tonyhallett.demomod;

import net.minecraft.block.Block;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TestStructureBlockFinder
{
    private final TestContext _context;

    public TestStructureBlockFinder(TestContext context){
        _context = context;
    }

    public List<BlockPos> findBlocksInTestStructure(Block block){
        List<BlockPos> results = new ArrayList<>();
        _context.forEachRelativePos(pos -> {
            var state = _context.getBlockState(pos);
            if (state.isOf(block)) {
                results.add(pos.mutableCopy());
            }
        });

        return results;
    }
}
