package net.cyberroadie.spike.filechange;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * User: olivier
 * Date: 27/07/2012
 */
public class MathexRepositoryTest {

    MathexRepository classToTest;

    @Before
    public void setup() throws IOException {
        String filePath = Thread.currentThread().getContextClassLoader().getResource("1-record.log").getPath();
        this.classToTest = new MathexRepository("", filePath);
    }

    @Test
    public void testReadRecords() throws Exception {
        List<String> lines = classToTest.readRecord();
        assertEquals(4, lines.size());
        lines = classToTest.readRecord();
        assertEquals(4, lines.size());
    }

    @Test
    public void testTail() throws IOException {
        String line = classToTest.readLineBackwards();
        assertEquals("--------------------------------------------------------------------------", line);
        line = classToTest.readLineBackwards();
        assertEquals("http://www.atarnotes.com/forum/index.php?topic=135541.msg561422", line);
        line = classToTest.readLineBackwards();
        assertEquals("\\int^1_3{\\int^{4-(x-3)^2}_0{(2xy)}} dydx", line);
    }
}
